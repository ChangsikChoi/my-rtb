#!/usr/bin/env bash
# 시나리오 B를 회차 사이 휴지를 두고 반복한다. 열 스로틀링이 있는 호스트(팬 없는 노트북 등)용.
#
# run_latency.sh를 REPEAT=1로 3번 실행한다. 회차 전에는 휴지(COOL_SEC, 첫 회차 제외)와 호스트 상태 확인(host_gate.sh)을 거치고,
# 회차가 끝나면 스택을 내려(down -v) 휴지 동안 호스트가 쉬게 한다. 워밍업은 기본 120초(WARMUP_SEC).
# 3회가 끝나면 회차마다 첫 단계의 요청당 Lettuce 스레드 CPU(최다 스레드 코어 ÷ 요청률)를 세 회차의 중앙값과 비교한다.
# 중앙값에서 어느 쪽으로든 15%를 넘게 벗어난 회차는 호스트 상태 이상(스로틀링, 백그라운드 작업 등)으로 보고
# 결과를 invalid-<이름>-outlier-runN으로 옮긴 뒤, 더 쉬고 한 번 다시 측정한다. 재측정 결과는 다시 검사만 하고 반복하지 않는다.
# 처음 쓰는 세션에서는 먼저 유휴 상태 기준값을 잰다: perf/scripts/host_gate.sh init
#
# 사용 (레포 루트에서):
#   perf/scripts/run_latency_cooled.sh <이름> <캠페인 수> <워밍업 요청률> "<RATES>"
#   perf/scripts/run_latency_cooled.sh baseline-pc2-c100 100 100 "200 250 300 325 350 375"
#
# 결과: perf/results/<이름>-run1..3 (env.json의 procedure에 회차 표시, 휴지 시간, 호스트 상태 확인 결과를 덧붙인다)
#
# 환경 변수:
#   COOL_SEC        회차 사이 휴지 (기본 480)
#   EXTRA_COOL_SEC  이상 회차 재측정 전 추가 휴지 (기본 600)
#   WARMUP_SEC      워밍업 시간 (기본 120)
#   그 밖의 run_latency.sh 변수(STEP_SEC, WIN_RATIO, BUILD 등)는 그대로 전달된다.

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

[[ $# -eq 4 ]] || {
  echo "사용법: $0 <이름> <캠페인 수> <워밍업 요청률> \"<RATES>\"" >&2
  exit 2
}
NAME="$1"
CAMPAIGNS="$2"
WARMUP="$3"
RATES="$4"
COOL_SEC="${COOL_SEC:-480}"
EXTRA_COOL_SEC="${EXTRA_COOL_SEC:-600}"
export WARMUP_SEC="${WARMUP_SEC:-120}"
OUTLIER_LIMIT=0.15

log() { echo "[cooled] $(date +%H:%M:%S) $*"; }

# 첫 단계의 요청당 Lettuce 스레드 CPU(ms)
per_request_ms() {
  jq -r '.steps[0] | (.bidder.busiestThread.cores / .rate * 1000 * 1000 | round / 1000)' "$1/summary.json"
}

# run_one <회차> <직전 휴지 초>: 호스트 상태 확인 → 측정 → 스택 내림
run_one() {
  local run_no="$1" cooled="$2" tmp="$NAME-r$1" gate
  log "호스트 상태 확인"
  gate="$(perf/scripts/host_gate.sh wait)" || { log "호스트 상태가 기준으로 돌아오지 않았습니다: $gate"; return 1; }
  WARMUP_RATE="$WARMUP" REPEAT=1 CAMPAIGNS="$CAMPAIGNS" RATES="$RATES" \
    perf/scripts/run_latency.sh "$tmp" || return 1
  mv "perf/results/$tmp-run1" "perf/results/$NAME-run$run_no"
  docker compose -f docker-compose.yml -f perf/docker-compose.perf.yml down -v --remove-orphans >/dev/null 2>&1 || true

  local env_file="perf/results/$NAME-run$run_no/env.json"
  jq --arg run "$run_no/3" --argjson cooled "$cooled" --argjson gate "$gate" \
    '.run = $run | .procedure = {repeatMode: "run_latency_cooled.sh (REPEAT=1 x3)", cooldownBeforeRunSec: $cooled, gate: $gate}' \
    "$env_file" >"$env_file.tmp" && mv "$env_file.tmp" "$env_file"
}

# deviation <값> <기준>: |값 - 기준| / 기준
deviation() { awk -v v="$1" -v r="$2" 'BEGIN { d = (v - r) / r; if (d < 0) d = -d; printf "%.3f", d }'; }

for run_no in 1 2 3; do
  cooled=0
  if ((run_no > 1)); then
    cooled="$COOL_SEC"
    log "${COOL_SEC}초 휴지"
    sleep "$COOL_SEC"
  fi
  run_one "$run_no" "$cooled" || exit 1
  log "run$run_no 첫 단계 요청당 Lettuce CPU $(per_request_ms "perf/results/$NAME-run$run_no")ms"
done

values=()
for run_no in 1 2 3; do values+=("$(per_request_ms "perf/results/$NAME-run$run_no")"); done
median="$(printf '%s\n' "${values[@]}" | sort -g | sed -n 2p)"
log "요청당 Lettuce CPU: ${values[*]} ms (중앙값 ${median}ms, 허용 ±$(awk -v l="$OUTLIER_LIMIT" 'BEGIN { print l * 100 }')%)"

for run_no in 1 2 3; do
  dev="$(deviation "${values[run_no - 1]}" "$median")"
  awk -v d="$dev" -v l="$OUTLIER_LIMIT" 'BEGIN { exit !(d > l) }' || continue

  log "⚠️ run$run_no 이상 (중앙값 대비 ${dev}) → invalid로 옮기고 ${EXTRA_COOL_SEC}초 휴지 후 재측정"
  mv "perf/results/$NAME-run$run_no" "perf/results/invalid-$NAME-outlier-run$run_no"
  sleep "$EXTRA_COOL_SEC"
  run_one "$run_no" "$EXTRA_COOL_SEC" || exit 1

  value="$(per_request_ms "perf/results/$NAME-run$run_no")"
  dev="$(deviation "$value" "$median")"
  if awk -v d="$dev" -v l="$OUTLIER_LIMIT" 'BEGIN { exit !(d > l) }'; then
    log "⚠️ run$run_no 재측정도 이상 (${value}ms, 중앙값 대비 ${dev}). 호스트 상태를 확인하고 다시 측정해야 한다"
  else
    log "run$run_no 재측정 ${value}ms (중앙값 대비 ${dev}) 정상"
  fi
done

log "완료 → perf/results/$NAME-run*"
for run_no in 1 2 3; do
  jq -r --arg n "$run_no" '"  run\($n): 최대 \(.maxSustainableRps // "없음") req/s | " +
    ([.steps[] | "r\(.rate) p99 \(.bidLatencyMs["p(99)"])"] | join(", "))' \
    "perf/results/$NAME-run$run_no/summary.json"
done
