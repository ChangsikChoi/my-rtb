#!/usr/bin/env bash
# 발열 특성 측정: 이 호스트에서 부하를 계속 주면 언제부터 느려지고, 멈춘 뒤 얼마 만에 회복되는지 잰다.
# 기준선 측정의 회차 길이·휴지 시간·시작 조건을 정하는 근거로 쓴다.
#
# 순서
#   0. 유휴 상태 프로브 (host_probe.sh) → 기준 속도
#   1. 스택 초기화 → 시드 → 워밍업
#   2. 부하 구간: 고정 요청률을 1분 단위 k6 실행으로 LOAD_MIN분 이어서 준다(분 사이 공백은 k6 시작 시간뿐).
#      분마다 p99와 bidder 스레드별 CPU(요청당 Lettuce CPU)를 남긴다. 처리량이 같으므로 요청당 CPU가 늘면 호스트가 느려진 것이다.
#   3. 회복 구간: 부하를 멈추고(스택은 유휴 상태로 유지) 1분마다 프로브를 RECOVERY_MIN분 잰다.
#   전 구간에 걸쳐 macOS 발열 상태, 컨테이너별 CPU, 호스트 상위 프로세스를 백그라운드로 기록한다.
#
# 사용 (레포 루트에서): perf/scripts/thermal_profile.sh <이름>
# 환경 변수: CAMPAIGNS(10) RATE(1250) LOAD_MIN(20) RECOVERY_MIN(25) WARMUP_RATE(250) WARMUP_SEC(60) IDLE_PROBES(5)
# 결과: perf/results/<이름>/ (idle.tsv, load.tsv, recovery.tsv, thermal.tsv, containers.csv, host-top.txt, env.json, 분별 k6 요약)

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
cd "$ROOT_DIR"

NAME="${1:?사용법: $0 <이름>}"
CAMPAIGNS="${CAMPAIGNS:-10}"
RATE="${RATE:-1250}"
LOAD_MIN="${LOAD_MIN:-20}"
RECOVERY_MIN="${RECOVERY_MIN:-25}"
WARMUP_RATE="${WARMUP_RATE:-250}"
WARMUP_SEC="${WARMUP_SEC:-60}"
IDLE_PROBES="${IDLE_PROBES:-5}"
BUDGET=100000000
TARGET_CPM=1000

require_commands docker k6 jq curl perl
DIR="perf/results/$NAME"
if [[ -e "$DIR" ]]; then error "$DIR 가 이미 있습니다"; fi
mkdir -p "$DIR"

probe_row() { # <구간 시작 epoch>
  local p
  p="$(perf/scripts/host_probe.sh 3)"
  printf '%s\t%s\t%s\t%s\n' "$(date +%H:%M:%S)" "$(($(date +%s) - $1))" \
    "$(jq -r .hostMs <<<"$p")" "$(jq -r .vmMs <<<"$p")"
}

# 백그라운드 기록 (전 구간)
#   thermal.tsv    macOS 발열 상태(NSProcessInfo.thermalState: 0 정상, 1 약간, 2 심각, 3 위험), 10초 간격
#   containers.csv 컨테이너별 CPU%(100% = 1 vCPU), docker stats 연속 호출
#   host-top.txt   호스트 CPU 사용 상위 프로세스, 30초 간격
SAMPLER_PIDS=()
start_samplers() {
  printf 'epoch\tthermal_state\n' >"$DIR/thermal.tsv"
  (while :; do
    printf '%s\t%s\n' "$(date +%s)" \
      "$(thermal_state)"
    sleep 10
  done) >>"$DIR/thermal.tsv" &
  SAMPLER_PIDS+=($!)

  echo "epoch,container,cpu_percent" >"$DIR/containers.csv"
  (while :; do
    docker stats --no-stream --format '{{.Name}},{{.CPUPerc}}' 2>/dev/null | tr -d '%' | sed "s/^/$(date +%s),/"
  done) >>"$DIR/containers.csv" &
  SAMPLER_PIDS+=($!)

  (while :; do
    echo "=== $(date +%s) $(date +%H:%M:%S)"
    top -l 2 -s 1 -n 8 -o cpu -stats pid,command,cpu | awk '/^Processes/ { n++ } n == 2' | grep -E 'CPU usage|^[0-9]'
    sleep 28
  done) >"$DIR/host-top.txt" 2>&1 &
  SAMPLER_PIDS+=($!)
}
stop_samplers() {
  local pid
  for pid in "${SAMPLER_PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done
}
trap stop_samplers EXIT

log "발열 특성 측정: $NAME (캠페인 $CAMPAIGNS, ${RATE} req/s × ${LOAD_MIN}분, 회복 관찰 ${RECOVERY_MIN}분)"
start_samplers

# 0. 유휴 기준
log "유휴 프로브 ${IDLE_PROBES}회"
printf 'time\tsec\thost_ms\tvm_ms\n' >"$DIR/idle.tsv"
t0="$(date +%s)"
for ((i = 1; i <= IDLE_PROBES; i++)); do
  probe_row "$t0" | tee -a "$DIR/idle.tsv" | sed 's/^/    /'
  if ((i < IDLE_PROBES)); then sleep 15; fi
done

# 1. 준비
reset_stack
wait_ready
record_env "$DIR" "thermal" "$(jq -n --argjson c "$CAMPAIGNS" --argjson r "$RATE" --argjson l "$LOAD_MIN" \
  --argjson rec "$RECOVERY_MIN" --argjson wr "$WARMUP_RATE" --argjson ws "$WARMUP_SEC" \
  '{scenario: "thermal", campaigns: $c, rate: $r, loadMin: $l, recoveryMin: $rec, warmupRate: $wr, warmupSec: $ws}')"
log "시드 (캠페인 ${CAMPAIGNS}개)"
CAMPAIGNS="$CAMPAIGNS" BUDGET="$BUDGET" TARGET_CPM="$TARGET_CPM" perf/scripts/seed.sh | sed 's/^/    /'
log "워밍업 (${WARMUP_RATE} req/s × ${WARMUP_SEC}s)"
k6 run -q --no-usage-report -e RATE="$WARMUP_RATE" -e DURATION_SEC="$WARMUP_SEC" \
  perf/k6/latency.js >"$DIR/warmup-output.txt" 2>&1 || true
grep '=== latency' -A1 "$DIR/warmup-output.txt" | tail -1 | sed 's/^/    /'

# 2. 부하 구간
clk_tck="$(bidder_clk_tck)"
log "부하 구간 시작"
printf 'minute\ttime\tachieved_rps\tp50_ms\tp95_ms\tp99_ms\tdropped\tlettuce_cores\tper_req_lettuce_ms\tbidder_thread_cores\n' >"$DIR/load.tsv"
before="$(mktemp)"; after="$(mktemp)"
for ((m = 1; m <= LOAD_MIN; m++)); do
  step="$DIR/load-m$m"
  mkdir -p "$step"
  bidder_thread_snapshot "$before"
  s="$(date +%s)"
  k6 run -q --no-usage-report -e RATE="$RATE" -e DURATION_SEC=60 -e RESULT_DIR="$step" \
    perf/k6/latency.js >"$step/k6-output.txt" 2>&1 || true
  bidder_thread_snapshot "$after"
  e="$(date +%s)"
  read -r lettuce total < <(bidder_thread_cores "$before" "$after" "$((e - s))" "$clk_tck")
  jq -r --arg m "$m" --arg t "$(date +%H:%M:%S)" --argjson l "$lettuce" --argjson tot "$total" --argjson r "$RATE" \
    '[$m, $t, .achievedRps, .bidLatencyMs["p(50)"], .bidLatencyMs["p(95)"], .bidLatencyMs["p(99)"],
      .counts.droppedIterations, $l, (($l / $r * 1000 * 1000 | round) / 1000), $tot] | @tsv' \
    "$step/k6-summary.json" | tee -a "$DIR/load.tsv" | sed 's/^/    /'
done
rm -f "$before" "$after"

# 3. 회복 구간
log "회복 구간 시작 (스택 유휴, 1분 간격 프로브)"
printf 'time\tsec\thost_ms\tvm_ms\n' >"$DIR/recovery.tsv"
t0="$(date +%s)"
for ((m = 0; m <= RECOVERY_MIN; m++)); do
  target=$((t0 + m * 60))
  now="$(date +%s)"
  if ((target > now)); then sleep $((target - now)); fi
  probe_row "$t0" | tee -a "$DIR/recovery.tsv" | sed 's/^/    /'
done

"${COMPOSE[@]}" logs --no-log-prefix bidder >"$DIR/bidder.log" 2>&1 || true
log "완료 → $DIR"
