#!/usr/bin/env bash
# 시나리오 B-지속: 고정 요청률을 HOLD_MIN분 동안 유지해 "계속 버틸 수 있는 처리량"을 잰다.
#
# 60초 단계 측정(run_latency.sh)에서는 입찰 후 30초 뒤 시작되는 만료 환불이 대부분 단계 뒤에 처리되어
# 환불 비용이 거의 반영되지 않는다. 이 측정은 입찰과 환불이 함께 처리되는 상태를 유지한 채 판정한다.
#
# 요청률마다 REPEAT회 측정하고, 회차 전에는 휴지(COOL_SEC, 첫 회차 제외)와 호스트 상태 확인(host_gate.sh)을 거친다.
# 회차가 끝나면 스택을 내려(down -v) 휴지 동안 호스트가 쉬게 한다.
#
# 회차: 스택 초기화 → 시드 → 워밍업 → 1분 단위 k6 실행을 HOLD_MIN번 이어서(분 사이 공백은 k6 시작 시간뿐)
# 분마다: p50/p95/p99, 드롭, 요청당 Lettuce CPU, bidder CPU(docker stats), 미환불 예약 수(budget_reserved 합 ÷ 1회 예약액)
# 판정: 모든 분에서 p99 ≤ SLA_P99_MS, 드롭 < 1%, 에러 0이면 통과.
#       포화(p99 > 1초 또는 드롭 > 1%)가 2분 연속이면 남은 시간을 생략하고 실패로 기록한다.
#
# 사용 (레포 루트에서):
#   perf/scripts/run_sustained.sh <이름> <캠페인 수> <워밍업 요청률> "<RATES>"
#   perf/scripts/run_sustained.sh sustained-pc2-c10 10 250 "600 700 800 900"            # 탐색: 요청률마다 1회, 실패하면 중단
#   REPEAT=3 STOP_ON_FAIL=0 perf/scripts/run_sustained.sh sustained-pc2-c10 10 250 "700"   # 확정: 3회
#
# 환경 변수:
#   HOLD_MIN (10) WARMUP_SEC (120) REPEAT (1) STOP_ON_FAIL (1: 어떤 요청률에서 한 회차라도 실패하면 이후 요청률 생략)
#   COOL_SEC (480) SLA_P99_MS (100) BUILD
#   SKIP_FIRST_GATE (0: 첫 회차 전에도 host_gate를 거친다)
#
# 결과: perf/results/<이름>-r<요청률>-runN/
#   env.json, seed.json, warmup-output.txt, sustained-summary.json, sustained-summary.tsv, containers.csv,
#   bidder-log-summary.txt, minute-mN/ (k6 요약)

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
cd "$ROOT_DIR"

[[ $# -eq 4 ]] || {
  echo "사용법: $0 <이름> <캠페인 수> <워밍업 요청률> \"<RATES>\"" >&2
  exit 2
}
NAME="$1"
CAMPAIGNS="$2"
WARMUP_RATE="$3"
read -r -a RATE_LIST <<<"$4"
HOLD_MIN="${HOLD_MIN:-10}"
WARMUP_SEC="${WARMUP_SEC:-120}"
REPEAT="${REPEAT:-1}"
STOP_ON_FAIL="${STOP_ON_FAIL:-1}"
COOL_SEC="${COOL_SEC:-480}"
SLA_P99_MS="${SLA_P99_MS:-100}"
SKIP_FIRST_GATE="${SKIP_FIRST_GATE:-0}"
SATURATION_P99_MS=1000
SATURATION_DROP=0.01
BUDGET=100000000
TARGET_CPM=1000

require_commands docker k6 jq curl perl

SAMPLER_PID=""
stop_sampler() {
  if [[ -n "$SAMPLER_PID" ]]; then
    kill "$SAMPLER_PID" 2>/dev/null || true
    wait "$SAMPLER_PID" 2>/dev/null || true
  fi
  SAMPLER_PID=""
}
trap stop_sampler EXIT

# 미환불 예약 수: 모든 캠페인의 budget_reserved 합 ÷ 1회 예약액
pending_reservations() {
  local price
  price="$(jq .impressionPriceMicro perf/.state/seed.json)"
  # shellcheck disable=SC2046  # 키에 공백이 없다(UUID)
  redis_cli MGET $(jq -r '.campaigns[] | "campaign:\(.):budget_reserved"' perf/.state/seed.json) |
    awk -v p="$price" '{ s += $1 } END { printf "%d\n", s / p }'
}

# run_one <요청률> <회차> <게이트 결과 JSON> → 통과면 0, 실패면 1
run_one() {
  local rate="$1" run_no="$2" gate="$3"
  local dir="perf/results/$NAME-r$rate-run$run_no"
  [[ -e "$dir" ]] && error "$dir 가 이미 있습니다"

  echo
  log "========== $NAME-r$rate-run$run_no (${rate} req/s × ${HOLD_MIN}분) =========="
  reset_stack
  wait_ready
  mkdir -p "$dir"
  record_env "$dir" "$run_no/$REPEAT" "$(jq -n \
    --argjson campaigns "$CAMPAIGNS" --argjson rate "$rate" --argjson holdMin "$HOLD_MIN" \
    --argjson warmupRate "$WARMUP_RATE" --argjson warmupSec "$WARMUP_SEC" --argjson coolSec "$COOL_SEC" \
    --argjson sla "$SLA_P99_MS" --argjson gate "$gate" \
    '{scenario: "sustained", campaigns: $campaigns, rate: $rate, holdMin: $holdMin, warmupRate: $warmupRate,
      warmupSec: $warmupSec, coolSec: $coolSec, slaP99Ms: $sla, winRatio: 0, gate: $gate}')"

  log "시드 (캠페인 ${CAMPAIGNS}개)"
  CAMPAIGNS="$CAMPAIGNS" BUDGET="$BUDGET" TARGET_CPM="$TARGET_CPM" perf/scripts/seed.sh | sed 's/^/    /'
  cp perf/.state/seed.json "$dir/seed.json"

  log "워밍업 (${WARMUP_RATE} req/s × ${WARMUP_SEC}s)"
  k6 run -q --no-usage-report -e RATE="$WARMUP_RATE" -e DURATION_SEC="$WARMUP_SEC" \
    perf/k6/latency.js >"$dir/warmup-output.txt" 2>&1 || true
  grep -A1 '=== latency' "$dir/warmup-output.txt" | tail -1 | sed 's/^/    /'

  echo "epoch,container,cpu_percent" >"$dir/containers.csv"
  (while :; do
    docker stats --no-stream --format '{{.Name}},{{.CPUPerc}}' 2>/dev/null | tr -d '%' | sed "s/^/$(date +%s),/"
  done) >>"$dir/containers.csv" &
  SAMPLER_PID=$!

  local clk_tck before after m s e lettuce total sat_streak=0 aborted=false
  clk_tck="$(bidder_clk_tck)"
  before="$(mktemp)"; after="$(mktemp)"
  : >"$dir/minutes.jsonl"
  for ((m = 1; m <= HOLD_MIN; m++)); do
    local step="$dir/minute-m$m"
    mkdir -p "$step"
    bidder_thread_snapshot "$before"
    s="$(date +%s)"
    k6 run -q --no-usage-report -e RATE="$rate" -e DURATION_SEC=60 -e RESULT_DIR="$step" \
      perf/k6/latency.js >"$step/k6-output.txt" 2>&1 || true
    e="$(date +%s)"
    bidder_thread_snapshot "$after"
    read -r lettuce total < <(bidder_thread_cores "$before" "$after" "$((e - s))" "$clk_tck")
    [[ -f "$step/k6-summary.json" ]] || error "k6 요약 파일이 없습니다: $step"

    local bidder_cpu pending
    bidder_cpu="$(awk -F, -v a="$s" -v b="$e" '$1 >= a && $1 < b && $2 ~ /bidder/ { t += $3; n++ }
      END { if (n) printf "%.1f", t / n; else print "null" }' "$dir/containers.csv")"
    pending="$(pending_reservations)"

    jq -c --argjson m "$m" --argjson s "$s" --argjson e "$e" --argjson l "$lettuce" --argjson tot "$total" \
      --argjson cpu "$bidder_cpu" --argjson pending "$pending" --argjson sla "$SLA_P99_MS" \
      --argjson satP99 "$SATURATION_P99_MS" --argjson satDrop "$SATURATION_DROP" \
      '. as $k | {
        minute: $m, startEpoch: $s, endEpoch: $e, rate: $k.rate, achievedRps: $k.achievedRps,
        bidLatencyMs: $k.bidLatencyMs, counts: $k.counts, droppedRatio: $k.droppedRatio,
        lettuceCores: $l, perReqLettuceMs: (($l / $k.rate * 1000 * 1000 | round) / 1000),
        bidderThreadCores: $tot, bidderCpuPercent: $cpu, pendingReservations: $pending,
        slaOk: ($k.bidLatencyMs["p(99)"] <= $sla and $k.droppedRatio < $satDrop and $k.counts.bidError == 0),
        saturated: ($k.bidLatencyMs["p(99)"] > $satP99 or $k.droppedRatio > $satDrop)
      }' "$step/k6-summary.json" >>"$dir/minutes.jsonl"

    tail -1 "$dir/minutes.jsonl" | jq -r '"    m\(.minute): 실제 \(.achievedRps)/s | p50 \(.bidLatencyMs["p(50)"]) p95 \(.bidLatencyMs["p(95)"]) p99 \(.bidLatencyMs["p(99)"])ms | drop \(.counts.droppedIterations) | bidder cpu \(.bidderCpuPercent)%, lettuce \(.perReqLettuceMs)ms/req | 미환불 예약 \(.pendingReservations) | \(if .saturated then "🔴 포화" elif .slaOk then "✅" else "⚠️ SLA 초과" end)"'

    if [[ "$(tail -1 "$dir/minutes.jsonl" | jq -r .saturated)" == "true" ]]; then
      sat_streak=$((sat_streak + 1))
    else
      sat_streak=0
    fi
    if ((sat_streak >= 2 && m < HOLD_MIN)); then
      log "포화 2분 연속 → 남은 $((HOLD_MIN - m))분 생략"
      aborted=true
      break
    fi
  done
  rm -f "$before" "$after"
  stop_sampler

  {
    redis_cli INFO memory | grep -E '^used_memory(_human|_peak_human)?:'
    redis_cli INFO keyspace
  } >"$dir/redis-info.txt"

  jq -s --argjson campaigns "$CAMPAIGNS" --argjson rate "$rate" --argjson hold "$HOLD_MIN" \
    --argjson sla "$SLA_P99_MS" --argjson aborted "$aborted" \
    '{campaigns: $campaigns, rate: $rate, holdMin: $hold, slaP99Ms: $sla, aborted: $aborted,
      minutesMeasured: length,
      sustainedOk: ((length == $hold) and all(.[]; .slaOk)),
      worstP99Ms: (map(.bidLatencyMs["p(99)"]) | max),
      minutes: .}' "$dir/minutes.jsonl" >"$dir/sustained-summary.json"
  rm -f "$dir/minutes.jsonl"

  {
    printf 'minute\tachieved_rps\tp50_ms\tp95_ms\tp99_ms\tdropped\terrors\tbidder_cpu_pct\tlettuce_cores\tper_req_lettuce_ms\tbidder_thread_cores\tpending_reservations\tsla_ok\tsaturated\n'
    jq -r '.minutes[] | [.minute, .achievedRps, .bidLatencyMs["p(50)"], .bidLatencyMs["p(95)"], .bidLatencyMs["p(99)"],
      .counts.droppedIterations, .counts.bidError, .bidderCpuPercent, .lettuceCores, .perReqLettuceMs,
      .bidderThreadCores, .pendingReservations, .slaOk, .saturated] | @tsv' "$dir/sustained-summary.json"
  } >"$dir/sustained-summary.tsv"

  "${COMPOSE[@]}" logs --no-log-prefix bidder >"$dir/bidder.log" 2>&1 || true
  perf/scripts/summarize_bidder_log.sh "$dir/bidder.log" >"$dir/bidder-log-summary.txt" || true

  # 휴지 동안 호스트가 쉬도록 스택을 내린다(다음 회차는 어차피 초기화한다).
  "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true

  local ok
  ok="$(jq -r .sustainedOk "$dir/sustained-summary.json")"
  log "판정: $([[ "$ok" == "true" ]] && echo "✅ ${HOLD_MIN}분 유지 통과" || echo "❌ 실패 (최악 p99 $(jq -r .worstP99Ms "$dir/sustained-summary.json")ms)")"
  [[ "$ok" == "true" ]]
}

log "시나리오 B-지속: $NAME (캠페인 ${CAMPAIGNS}개, RATES=\"${RATE_LIST[*]}\", ${HOLD_MIN}분 유지, 요청률당 ${REPEAT}회)"
first=true
for rate in "${RATE_LIST[@]}"; do
  failed=0
  for ((k = 1; k <= REPEAT; k++)); do
    if [[ "$first" == "true" && "$SKIP_FIRST_GATE" == "1" ]]; then
      gate='{"skipped": true}'
    else
      if [[ "$first" != "true" ]]; then
        log "${COOL_SEC}초 휴지"
        sleep "$COOL_SEC"
      fi
      log "호스트 상태 확인"
      gate="$(perf/scripts/host_gate.sh wait)" || error "호스트 상태가 기준으로 돌아오지 않았습니다: $gate"
    fi
    first=false
    run_one "$rate" "$k" "$gate" || failed=$((failed + 1))
  done
  log "r$rate: ${REPEAT}회 중 $((REPEAT - failed))회 통과"
  if ((failed > 0)) && [[ "$STOP_ON_FAIL" == "1" ]]; then
    log "실패가 있어 이후 요청률은 생략"
    break
  fi
done
log "완료 → perf/results/$NAME-r*"
