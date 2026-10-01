#!/usr/bin/env bash
# 시나리오 B: 활성 캠페인 수(N)를 고정하고 요청률을 단계적으로 올리며 지연시간과 서버 지표를 측정한다.
#
# 회차마다: 스택 초기화 → 기동 대기 → 캠페인 N개 시드 → 워밍업 → 요청률 단계별 측정
# 단계마다: Redis 통계 초기화 → bidder CPU 샘플링하며 k6 부하 → bidder 스레드별 CPU, Redis 명령·CPU·메모리 수집 → 포화 판정
#
# 사용 예:
#   CAMPAIGNS=10   perf/scripts/run_latency.sh latency-c10
#   CAMPAIGNS=100  perf/scripts/run_latency.sh latency-c100
#   CAMPAIGNS=1000 RATES="25 50 100 200 400" perf/scripts/run_latency.sh latency-c1000
#
# 환경 변수:
#   CAMPAIGNS           활성 캠페인 수 (기본 100)
#   RATES               측정할 초당 요청 수 목록, 공백 구분, 오름차순 (기본 "100 250 500 750 1000")
#   STEP_SEC            단계별 측정 시간 (기본 60)
#   COOLDOWN_SEC        단계 사이 대기 시간 (기본 10)
#   WARMUP_RATE         워밍업 요청률 (기본 RATES의 첫 값과 250 중 작은 값)
#   WARMUP_SEC          워밍업 시간, 캠페인 시드 후 입찰 성공 경로까지 데운다 (기본 60)
#   WIN_RATIO           성공 입찰 중 win 호출 비율 (기본 0: 모든 예약은 30초 뒤 만료 환불)
#   BUDGET              캠페인당 예산, 정수 원 (기본 100000000: 측정 중 소진되지 않는 크기)
#   TARGET_CPM          목표 CPM, 정수 원 (기본 1000)
#   SLA_P99_MS          처리 가능 판정 기준 p99 (기본 100: RTB 응답 제한)
#   SATURATION_P99_MS   이 p99를 넘으면 포화로 보고 이후 단계를 건너뜀 (기본 1000)
#   SATURATION_DROP     목표 요청 중 드롭 비율이 이 값을 넘으면 포화 (기본 0.01)
#   REPEAT              반복 횟수 (기본 1)
#   BUILD, READY_TIMEOUT  lib.sh 참고
#
# 결과: perf/results/<이름>-runN/
#   env.json, seed.json, warmup-output.txt, summary.json, summary.tsv, bidder-log-summary.txt
#   step-r<요청률>/  k6-summary.json, k6-raw-summary.json, k6-output.txt,
#                    docker-stats.csv, bidder-thread-cpu.tsv, redis-commandstats.txt, redis-info.txt,
#                    step-summary.json
#
# 종료 코드: 측정이 끝까지 진행되면 0 (포화는 실패가 아니라 측정 결과로 기록한다)

set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

NAME="${1:-}"
CAMPAIGNS="${CAMPAIGNS:-100}"
RATES="${RATES:-100 250 500 750 1000}"
STEP_SEC="${STEP_SEC:-60}"
COOLDOWN_SEC="${COOLDOWN_SEC:-10}"
read -r -a RATE_LIST <<<"$RATES"
# 워밍업은 처리 한계보다 충분히 낮은 요청률로 한다. 콜드 JVM에 한계 근처 부하를 바로 주면
# JIT 컴파일 전 쌓인 대기열이 해소되지 못해 이후 단계까지 포화 상태로 남는다.
WARMUP_MAX_RATE=250
WARMUP_RATE="${WARMUP_RATE:-$((RATE_LIST[0] < WARMUP_MAX_RATE ? RATE_LIST[0] : WARMUP_MAX_RATE))}"
WARMUP_SEC="${WARMUP_SEC:-60}"
WIN_RATIO="${WIN_RATIO:-0}"
BUDGET="${BUDGET:-100000000}"
TARGET_CPM="${TARGET_CPM:-1000}"
SLA_P99_MS="${SLA_P99_MS:-100}"
SATURATION_P99_MS="${SATURATION_P99_MS:-1000}"
SATURATION_DROP="${SATURATION_DROP:-0.01}"
REPEAT="${REPEAT:-1}"

[[ -n "$NAME" ]] || error "결과 이름을 지정하세요. 예: CAMPAIGNS=100 perf/scripts/run_latency.sh latency-c100"
[[ "$REPEAT" =~ ^[1-9][0-9]*$ ]] || error "REPEAT는 양의 정수여야 합니다: $REPEAT"
[[ "${#RATE_LIST[@]}" -gt 0 ]] || error "RATES가 비어 있습니다"
for r in "${RATE_LIST[@]}"; do
  [[ "$r" =~ ^[1-9][0-9]*$ ]] || error "RATES는 양의 정수 목록이어야 합니다: $RATES"
done
require_commands docker k6 jq curl awk

for ((i = 1; i <= REPEAT; i++)); do
  dir="$ROOT_DIR/perf/results/$NAME-run$i"
  [[ ! -e "$dir" ]] || error "결과 디렉토리가 이미 있습니다: ${dir#"$ROOT_DIR"/}"
done

cd "$ROOT_DIR"

redis_cli() {
  "${COMPOSE[@]}" exec -T redis redis-cli "$@" | tr -d '\r'
}

redis_info_field() {
  # redis_info_field <section> <field>
  redis_cli INFO "$1" | sed -n "s/^$2://p"
}

# ---------------------------------------------------------------------------
# bidder CPU 샘플러 (docker stats를 반복 호출해 CSV로 남긴다)
# ---------------------------------------------------------------------------
SAMPLER_PID=""
SAMPLER_FLAG=""

start_sampler() {
  local out="$1" container="$2"
  SAMPLER_FLAG="$(mktemp)"
  echo "epoch,container,cpu_percent,mem_usage" >"$out"
  (
    while [[ -f "$SAMPLER_FLAG" ]]; do
      docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}}' "$container" 2>/dev/null |
        sed "s/^/$(date +%s),/" >>"$out" || true
    done
  ) &
  SAMPLER_PID=$!
}

stop_sampler() {
  rm -f "$SAMPLER_FLAG"
  [[ -n "$SAMPLER_PID" ]] && wait "$SAMPLER_PID" 2>/dev/null || true
  SAMPLER_PID=""
}

trap 'stop_sampler' EXIT

# ---------------------------------------------------------------------------
# 단계 실행
# ---------------------------------------------------------------------------

# commandstats를 {command: {calls, usec}} JSON으로 변환한다.
commandstats_json() {
  sed -n 's/^cmdstat_\([^:]*\):calls=\([0-9]*\),usec=\([0-9]*\).*/\1 \2 \3/p' "$1" |
    jq -R -s 'split("\n") | map(select(length > 0) | split(" ")) |
      map({key: .[0], value: {calls: (.[1] | tonumber), usec: (.[2] | tonumber)}}) | from_entries'
}

# bidder JVM(PID 1)의 스레드별 누적 CPU 시간(user+sys, clock tick)을 "tid|이름|tick" 형식으로 남긴다.
thread_snapshot() {
  "${COMPOSE[@]}" exec -T bidder sh -c '
    for t in /proc/1/task/*; do
      name=$(cat "$t/comm" 2>/dev/null) || continue
      ticks=$(cut -d")" -f2- "$t/stat" 2>/dev/null | awk "{print \$12 + \$13}") || continue
      echo "${t##*/}|$name|$ticks"
    done' | tr -d '\r' >"$1"
}

# 두 스냅샷의 차이를 스레드 그룹별 사용 코어로 환산한다.
# 스레드 이름 끝의 번호를 지워 같은 풀의 스레드를 한 그룹으로 묶고, 그룹 안에서 가장 바쁜 스레드의 사용량도 남긴다.
# 출력(TSV): 그룹 이름, 그룹 합계 코어, 스레드 수, 가장 바쁜 스레드 코어
thread_cpu_tsv() {
  local before="$1" after="$2" seconds="$3" clk_tck="$4"
  awk -F'|' -v secs="$seconds" -v hz="$clk_tck" '
    NR == FNR { base[$1] = $3; next }
    ($1 in base) {
      d = $3 - base[$1]
      if (d <= 0) next
      group = $2
      sub(/[-#_ ]*[0-9]+$/, "", group)
      sum[group] += d; count[group]++
      if (d > top[group]) top[group] = d
    }
    END {
      for (g in sum) printf "%s\t%.3f\t%d\t%.3f\n", g, sum[g] / hz / secs, count[g], top[g] / hz / secs
    }' "$before" "$after" | sort -t $'\t' -k2,2 -rn
}

run_step() {
  local run_dir="$1" rate="$2"
  local step_dir="$run_dir/step-r$rate"
  mkdir -p "$step_dir"

  local bidder_container clk_tck
  bidder_container="$("${COMPOSE[@]}" ps -q bidder)"
  clk_tck="$("${COMPOSE[@]}" exec -T bidder getconf CLK_TCK 2>/dev/null | tr -d '\r' || true)"
  [[ "$clk_tck" =~ ^[0-9]+$ ]] || clk_tck=100

  redis_cli CONFIG RESETSTAT >/dev/null
  local cpu_before
  cpu_before="$(redis_cli INFO cpu | awk -F: '/^used_cpu_(sys|user):/ {s += $2} END {print s}')"

  local threads_before threads_after started finished
  threads_before="$(mktemp)"
  threads_after="$(mktemp)"
  thread_snapshot "$threads_before"
  started="$(date +%s)"

  start_sampler "$step_dir/docker-stats.csv" "$bidder_container"
  k6 run -q --no-usage-report \
    -e RATE="$rate" -e DURATION_SEC="$STEP_SEC" -e WIN_RATIO="$WIN_RATIO" -e RESULT_DIR="$step_dir" \
    perf/k6/latency.js >"$step_dir/k6-output.txt" 2>&1 || true
  stop_sampler

  thread_snapshot "$threads_after"
  finished="$(date +%s)"
  {
    printf 'thread_group\tcores\tthreads\tbusiest_thread_cores\n'
    thread_cpu_tsv "$threads_before" "$threads_after" "$((finished - started))" "$clk_tck"
  } >"$step_dir/bidder-thread-cpu.tsv"
  rm -f "$threads_before" "$threads_after"

  [[ -f "$step_dir/k6-summary.json" ]] || {
    tail -20 "$step_dir/k6-output.txt" >&2
    error "k6 요약 파일이 생성되지 않았습니다 (출력: $step_dir/k6-output.txt)"
  }

  local cpu_after
  cpu_after="$(redis_cli INFO cpu | awk -F: '/^used_cpu_(sys|user):/ {s += $2} END {print s}')"
  redis_cli INFO commandstats >"$step_dir/redis-commandstats.txt"
  {
    redis_cli INFO memory | grep -E '^used_memory(_human|_peak_human)?:'
    redis_cli INFO keyspace
  } >"$step_dir/redis-info.txt"

  local used_memory
  used_memory="$(sed -n 's/^used_memory://p' "$step_dir/redis-info.txt")"

  # bidder CPU: docker stats 기준(100% = 1 vCPU). 첫 샘플은 부하 시작 직전일 수 있어 제외한다.
  local bidder_cpu
  bidder_cpu="$(awk -F, 'NR > 2 { gsub(/%/, "", $3); s += $3; n++; if ($3 > m) m = $3 }
    END { if (n > 0) printf "{\"avgPercent\": %.1f, \"maxPercent\": %.1f, \"samples\": %d}", s / n, m, n;
          else printf "{\"avgPercent\": null, \"maxPercent\": null, \"samples\": 0}" }' "$step_dir/docker-stats.csv")"

  local threads_json
  threads_json="$(tail -n +2 "$step_dir/bidder-thread-cpu.tsv" | jq -R -s '
    split("\n") | map(select(length > 0) | split("\t") |
      {group: .[0], cores: (.[1] | tonumber), threads: (.[2] | tonumber), busiestThreadCores: (.[3] | tonumber)})')"

  jq -n \
    --slurpfile k6 "$step_dir/k6-summary.json" \
    --argjson cmds "$(commandstats_json "$step_dir/redis-commandstats.txt")" \
    --argjson threads "$threads_json" \
    --argjson redisCpuSec "$(awk -v a="$cpu_after" -v b="$cpu_before" 'BEGIN { printf "%.3f", a - b }')" \
    --argjson usedMemory "${used_memory:-0}" \
    --argjson bidderCpu "$bidder_cpu" \
    --argjson campaigns "$CAMPAIGNS" \
    --argjson sla "$SLA_P99_MS" \
    --argjson satP99 "$SATURATION_P99_MS" \
    --argjson satDrop "$SATURATION_DROP" \
    '$k6[0] as $k |
     ($cmds | to_entries | map(.value.calls) | add // 0) as $total |
     (if $k.bidRequests > 0 then $k.bidRequests else 1 end) as $req |
     ($k.bidLatencyMs["p(99)"]) as $p99 |
     {
       rate: $k.rate,
       campaigns: $campaigns,
       achievedRps: $k.achievedRps,
       bidLatencyMs: $k.bidLatencyMs,
       counts: $k.counts,
       droppedRatio: $k.droppedRatio,
       redis: {
         totalCalls: $total,
         callsPerBid: (($total / $req) * 10 | round / 10),
         hgetallPerBid: ((($cmds.hgetall.calls // 0) / $req) * 10 | round / 10),
         commands: ($cmds | with_entries(.value = .value.calls)),
         cpuCoresUsed: (($redisCpuSec / $k.durationSec) * 100 | round / 100),
         usedMemoryMiB: (($usedMemory / 1048576) * 10 | round / 10)
       },
       bidder: {
         cpu: $bidderCpu,
         threadGroups: $threads,
         busiestThread: ($threads | max_by(.busiestThreadCores) // null | if . then {group, cores: .busiestThreadCores} else null end)
       },
       slaOk: ($p99 <= $sla and $k.droppedRatio < $satDrop and $k.counts.bidError == 0),
       saturated: ($p99 > $satP99 or $k.droppedRatio > $satDrop)
     }' >"$step_dir/step-summary.json"

  jq -r '"    r\(.rate): 실제 \(.achievedRps)/s | p50 \(.bidLatencyMs["p(50)"]) p95 \(.bidLatencyMs["p(95)"]) p99 \(.bidLatencyMs["p(99)"])ms | drop \(.counts.droppedIterations) err \(.counts.bidError) | redis \(.redis.callsPerBid) cmd/req (hgetall \(.redis.hgetallPerBid)), \(.redis.cpuCoresUsed) core | bidder cpu avg \(.bidder.cpu.avgPercent)%, 최다 스레드 \(.bidder.busiestThread.group) \(.bidder.busiestThread.cores) core | \(if .saturated then "🔴 포화" elif .slaOk then "✅ SLA" else "⚠️ SLA 초과" end)"' \
    "$step_dir/step-summary.json"
}

run_once() {
  local run_no="$1"
  local dir="perf/results/$NAME-run$run_no"

  echo
  log "========== $NAME-run$run_no ($run_no/$REPEAT) =========="
  reset_stack
  wait_ready

  mkdir -p "$dir"
  record_env "$dir" "$run_no/$REPEAT" "$(jq -n \
    --argjson campaigns "$CAMPAIGNS" --arg rates "$RATES" --argjson stepSec "$STEP_SEC" \
    --argjson cooldownSec "$COOLDOWN_SEC" --argjson warmupRate "$WARMUP_RATE" --argjson warmupSec "$WARMUP_SEC" \
    --argjson winRatio "$WIN_RATIO" --argjson budget "$BUDGET" --argjson targetCpm "$TARGET_CPM" \
    --argjson slaP99Ms "$SLA_P99_MS" --argjson saturationP99Ms "$SATURATION_P99_MS" --argjson saturationDrop "$SATURATION_DROP" \
    '{scenario: "latency", campaigns: $campaigns, rates: ($rates | split(" ") | map(select(. != "") | tonumber)),
      stepSec: $stepSec, cooldownSec: $cooldownSec, warmupRate: $warmupRate, warmupSec: $warmupSec,
      winRatio: $winRatio, budget: $budget, targetCpm: $targetCpm,
      slaP99Ms: $slaP99Ms, saturationP99Ms: $saturationP99Ms, saturationDrop: $saturationDrop}')"

  log "시드 (캠페인 ${CAMPAIGNS}개, 캠페인당 예산 ${BUDGET})"
  CAMPAIGNS="$CAMPAIGNS" BUDGET="$BUDGET" TARGET_CPM="$TARGET_CPM" perf/scripts/seed.sh | sed 's/^/    /'
  cp perf/.state/seed.json "$dir/seed.json"

  log "워밍업 (${WARMUP_RATE} req/s × ${WARMUP_SEC}s, 입찰 성공 경로 포함)"
  k6 run -q --no-usage-report -e RATE="$WARMUP_RATE" -e DURATION_SEC="$WARMUP_SEC" -e WIN_RATIO="$WIN_RATIO" \
    perf/k6/latency.js >"$dir/warmup-output.txt" 2>&1 || true
  sed -n '/=== latency/,$p' "$dir/warmup-output.txt" | grep -v '^time=' | sed 's/^/    /'

  local rate saturated=false
  for rate in "${RATE_LIST[@]}"; do
    if [[ "$saturated" == "true" ]]; then
      log "r$rate 건너뜀 (이전 단계에서 포화)"
      continue
    fi
    sleep "$COOLDOWN_SEC"
    log "단계 r$rate (${rate} req/s × ${STEP_SEC}s)"
    run_step "$dir" "$rate"
    saturated="$(jq -r '.saturated' "$dir/step-r$rate/step-summary.json")"
  done

  # 단계 결과를 회차 요약으로 모은다.
  local steps=()
  for rate in "${RATE_LIST[@]}"; do
    [[ -f "$dir/step-r$rate/step-summary.json" ]] && steps+=("$dir/step-r$rate/step-summary.json")
  done
  jq -s --argjson sla "$SLA_P99_MS" --argjson campaigns "$CAMPAIGNS" \
    '{campaigns: $campaigns, slaP99Ms: $sla,
      maxSustainableRps: (map(select(.slaOk) | .rate) | max),
      steps: .}' "${steps[@]}" >"$dir/summary.json"

  {
    printf 'rate\tachieved_rps\tp50_ms\tp95_ms\tp99_ms\tdropped\terrors\tredis_calls_per_bid\thgetall_per_bid\tredis_cores\tbidder_cpu_avg_pct\tbidder_cpu_max_pct\tbusiest_thread\tbusiest_thread_cores\tsla_ok\tsaturated\n'
    jq -r '.steps[] | [.rate, .achievedRps, .bidLatencyMs["p(50)"], .bidLatencyMs["p(95)"], .bidLatencyMs["p(99)"],
      .counts.droppedIterations, .counts.bidError, .redis.callsPerBid, .redis.hgetallPerBid, .redis.cpuCoresUsed,
      .bidder.cpu.avgPercent, .bidder.cpu.maxPercent, .bidder.busiestThread.group, .bidder.busiestThread.cores,
      .slaOk, .saturated] | @tsv' "$dir/summary.json"
  } >"$dir/summary.tsv"

  "${COMPOSE[@]}" logs --no-log-prefix bidder >"$dir/bidder.log" 2>&1 || true
  perf/scripts/summarize_bidder_log.sh "$dir/bidder.log" >"$dir/bidder-log-summary.txt" || true

  echo
  column -t -s $'\t' "$dir/summary.tsv" | sed 's/^/    /'
  log "p99 ≤ ${SLA_P99_MS}ms 최대 처리량: $(jq -r '.maxSustainableRps // "없음"' "$dir/summary.json") req/s (캠페인 ${CAMPAIGNS}개)"
}

# ---------------------------------------------------------------------------
# 실행
# ---------------------------------------------------------------------------

log "시나리오 B: $NAME × $REPEAT회 (캠페인 ${CAMPAIGNS}개, RATES=\"$RATES\", 단계 ${STEP_SEC}s, WIN_RATIO=$WIN_RATIO)"
if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  log "⚠️ 커밋되지 않은 변경이 있습니다. env.json에 변경 파일 수가 기록됩니다."
fi

for ((i = 1; i <= REPEAT; i++)); do
  run_once "$i"
done

echo
log "완료 → perf/results/$NAME-run*"
