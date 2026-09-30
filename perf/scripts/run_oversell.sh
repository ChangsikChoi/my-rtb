#!/usr/bin/env bash
# 시나리오 A(예산 초과 예약 검증)를 초기화부터 검증까지 한 번에 실행한다.
#
# 회차마다: 스택 초기화 → 기동 대기 → 워밍업 → 시드 → 부하 → Redis 검증 → 환경·로그 기록
#
# 사용 예:
#   perf/scripts/run_oversell.sh oversell-r500                 # perf/results/oversell-r500-run1
#   REPEAT=3 perf/scripts/run_oversell.sh oversell-r500        # run1~run3
#   REPEAT=3 RATE=2000 perf/scripts/run_oversell.sh oversell-r2000
#   BUILD=1 perf/scripts/run_oversell.sh oversell-r500         # 코드 변경 후 이미지 재빌드
#
# 환경 변수:
#   REPEAT         반복 횟수 (기본 1)
#   RATE           초당 입찰 요청 수 (기본 500)
#   DURATION_SEC   부하 시간, 예약 TTL 30초 미만 (기본 20)
#   WIN_RATIO      성공 입찰 중 win을 호출할 비율 (기본 0.5)
#   BUDGET         캠페인 예산, 정수 원 (기본 100)
#   TARGET_CPM     목표 CPM, 정수 원 (기본 1000)
#   WARMUP_RATE    워밍업 초당 요청 수 (기본 RATE와 동일)
#   WARMUP_SEC     워밍업 시간 (기본 30)
#   BUILD          1이면 기동 시 이미지를 재빌드 (기본 0)
#   READY_TIMEOUT  서비스 준비 대기 최대 시간 (기본 180)
#
# 결과: perf/results/<이름>-runN/ 에 다음 파일을 남긴다.
#   env.json                측정 환경 (커밋, 호스트, 컨테이너 제한, 파라미터)
#   warmup-output.txt       워밍업 출력
#   seed.json               시드 정보
#   k6-output.txt           부하 출력
#   oversell-summary.json   부하 결과 요약
#   k6-raw-summary.json     k6 전체 지표
#   verify-output.txt       검증 출력
#   verify-result.json      검증 결과
#   bidder.log              bidder 컨테이너 로그 (용량이 커서 git 제외)
#   bidder-log-summary.txt  로그 레벨별 건수와 WARN/ERROR 줄
#
# 종료 코드: 모든 회차가 통과하면 0, 하나라도 실패하면 1

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/perf/docker-compose.perf.yml")

NAME="${1:-}"
REPEAT="${REPEAT:-1}"
RATE="${RATE:-500}"
DURATION_SEC="${DURATION_SEC:-20}"
WIN_RATIO="${WIN_RATIO:-0.5}"
BUDGET="${BUDGET:-100}"
TARGET_CPM="${TARGET_CPM:-1000}"
WARMUP_RATE="${WARMUP_RATE:-$RATE}"
WARMUP_SEC="${WARMUP_SEC:-30}"
BUILD="${BUILD:-0}"
READY_TIMEOUT="${READY_TIMEOUT:-180}"

log() { echo "[run] $(date +%H:%M:%S) $*"; }
error() {
  echo "[run] ERROR: $*" >&2
  exit 2
}

[[ -n "$NAME" ]] || error "결과 이름을 지정하세요. 예: perf/scripts/run_oversell.sh oversell-r500"
[[ "$REPEAT" =~ ^[1-9][0-9]*$ ]] || error "REPEAT는 양의 정수여야 합니다: $REPEAT"
for cmd in docker k6 jq curl; do
  command -v "$cmd" >/dev/null || error "$cmd 가 필요합니다"
done

# 기존 결과를 덮어쓰지 않도록 시작 전에 모든 회차의 디렉토리를 확인한다.
for ((i = 1; i <= REPEAT; i++)); do
  dir="$ROOT_DIR/perf/results/$NAME-run$i"
  [[ ! -e "$dir" ]] || error "결과 디렉토리가 이미 있습니다: ${dir#"$ROOT_DIR"/}"
done

cd "$ROOT_DIR"

# ---------------------------------------------------------------------------
# 단계별 함수
# ---------------------------------------------------------------------------

reset_stack() {
  log "스택 초기화 (down -v)"
  "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1

  local up_args=(up -d)
  [[ "$BUILD" == "1" ]] && up_args+=(--build)
  log "스택 기동 (측정용 리소스 제한 적용$([[ "$BUILD" == "1" ]] && echo ', 이미지 재빌드'))"
  "${COMPOSE[@]}" "${up_args[@]}" >/dev/null 2>&1 || {
    "${COMPOSE[@]}" "${up_args[@]}"
    error "스택 기동 실패"
  }
}

wait_ready() {
  log "서비스 준비 대기 (최대 ${READY_TIMEOUT}초)"
  local deadline=$(($(date +%s) + READY_TIMEOUT))
  local bidder="" manager="" keyspace="" consumer=0

  while (($(date +%s) < deadline)); do
    bidder="$(curl -s localhost:8080/actuator/health || true)"
    manager="$(curl -s -o /dev/null -w '%{http_code}' localhost:8088/api/campaigns || true)"
    keyspace="$("${COMPOSE[@]}" exec -T redis redis-cli CONFIG GET notify-keyspace-events 2>/dev/null | tail -1 | tr -d '\r' || true)"
    consumer="$("${COMPOSE[@]}" logs --no-log-prefix log-consumer 2>/dev/null | grep -c 'partitions assigned' || true)"

    if [[ "$bidder" == *'"UP"'* && "$manager" != "000" && "$keyspace" == *E* && "$consumer" -gt 0 ]]; then
      log "준비 완료 (bidder UP, ad_manager HTTP $manager, keyspace '$keyspace', log-consumer 파티션 할당)"
      return 0
    fi
    sleep 3
  done

  echo "  bidder=$bidder ad_manager=$manager keyspace='$keyspace' log-consumer 할당 로그=$consumer" >&2
  "${COMPOSE[@]}" ps >&2
  error "서비스 준비 시간 초과"
}

record_env() {
  local dir="$1" run_no="$2"
  local limits='{}'
  local svc id
  for svc in bidder redis kafka schema-registry postgres ad-manager log-consumer; do
    id="$("${COMPOSE[@]}" ps -q "$svc")"
    limits="$(jq -c \
      --arg svc "$svc" \
      --argjson nano "$(docker inspect -f '{{.HostConfig.NanoCpus}}' "$id")" \
      --argjson mem "$(docker inspect -f '{{.HostConfig.Memory}}' "$id")" \
      --arg java "$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$id" | sed -n 's/^JAVA_TOOL_OPTIONS=//p')" \
      '. + {($svc): {cpus: ($nano / 1e9), memoryMiB: ($mem / 1048576), javaToolOptions: (if $java == "" then null else $java end)}}' \
      <<<"$limits")"
  done

  jq -n \
    --arg startedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --arg run "$run_no/$REPEAT" \
    --arg commit "$(git rev-parse HEAD)" \
    --argjson trackedChanges "$(git status --porcelain --untracked-files=no | grep -c . || true)" \
    --arg hostCpu "$(sysctl -n machdep.cpu.brand_string 2>/dev/null || grep -m1 'model name' /proc/cpuinfo | cut -d: -f2- || echo unknown)" \
    --arg hostCores "$(sysctl -n hw.ncpu 2>/dev/null || nproc)" \
    --arg dockerVm "$(docker info --format '{{.NCPU}} CPU / {{.MemTotal}} bytes')" \
    --arg k6 "$(k6 version | head -1)" \
    --argjson limits "$limits" \
    --argjson rate "$RATE" \
    --argjson durationSec "$DURATION_SEC" \
    --argjson winRatio "$WIN_RATIO" \
    --argjson budget "$BUDGET" \
    --argjson targetCpm "$TARGET_CPM" \
    --argjson warmupRate "$WARMUP_RATE" \
    --argjson warmupSec "$WARMUP_SEC" \
    '{
      startedAt: $startedAt,
      run: $run,
      git: {commit: $commit, trackedChanges: $trackedChanges},
      host: {cpu: $hostCpu, cores: $hostCores, dockerVm: $dockerVm, k6: $k6},
      containerLimits: $limits,
      params: {rate: $rate, durationSec: $durationSec, winRatio: $winRatio, budget: $budget,
               targetCpm: $targetCpm, warmupRate: $warmupRate, warmupSec: $warmupSec}
    }' >"$dir/env.json"
}

run_once() {
  local run_no="$1"
  local dir="perf/results/$NAME-run$run_no"
  local status=0

  echo
  log "========== $NAME-run$run_no ($run_no/$REPEAT) =========="
  reset_stack
  wait_ready

  mkdir -p "$dir"
  record_env "$dir" "$run_no"

  log "워밍업 (${WARMUP_RATE} req/s × ${WARMUP_SEC}s, 활성 캠페인 없음)"
  if ! k6 run -q --no-usage-report -e RATE="$WARMUP_RATE" -e DURATION_SEC="$WARMUP_SEC" \
    perf/k6/warmup.js >"$dir/warmup-output.txt" 2>&1; then
    tail -20 "$dir/warmup-output.txt" >&2
    error "워밍업 실패 (출력: $dir/warmup-output.txt)"
  fi
  sed -n '/=== warmup/,$p' "$dir/warmup-output.txt" | grep -v '^time=' | sed 's/^/    /'

  log "시드 (예산 ${BUDGET}, CPM ${TARGET_CPM})"
  CAMPAIGNS=1 BUDGET="$BUDGET" TARGET_CPM="$TARGET_CPM" perf/scripts/seed.sh | sed 's/^/    /'
  cp perf/.state/seed.json "$dir/seed.json"

  log "부하 (${RATE} req/s × ${DURATION_SEC}s, win 비율 ${WIN_RATIO})"
  k6 run -q --no-usage-report \
    -e RATE="$RATE" -e DURATION_SEC="$DURATION_SEC" -e WIN_RATIO="$WIN_RATIO" -e RESULT_DIR="$dir" \
    perf/k6/oversell.js >"$dir/k6-output.txt" 2>&1 || status=1
  sed -n '/=== oversell/,/입찰 지연/p' "$dir/k6-output.txt" | sed 's/^/    /'
  [[ -f "$dir/oversell-summary.json" ]] || {
    tail -20 "$dir/k6-output.txt" >&2
    error "부하 요약 파일이 생성되지 않았습니다 (출력: $dir/k6-output.txt)"
  }

  log "검증 (예약 TTL 경과 후 Redis 잔액 확인)"
  RESULT_DIR="$dir" perf/scripts/verify_budget.sh >"$dir/verify-output.txt" 2>&1 || status=1
  sed -n '/=== 예산 정합성/,$p' "$dir/verify-output.txt" | sed 's/^/    /'

  "${COMPOSE[@]}" logs --no-log-prefix bidder >"$dir/bidder.log" 2>&1 || true
  perf/scripts/summarize_bidder_log.sh "$dir/bidder.log" >"$dir/bidder-log-summary.txt" || true

  return "$status"
}

# ---------------------------------------------------------------------------
# 실행
# ---------------------------------------------------------------------------

log "시나리오 A: $NAME × $REPEAT회 (RATE=$RATE, DURATION_SEC=$DURATION_SEC, WIN_RATIO=$WIN_RATIO, BUDGET=$BUDGET, TARGET_CPM=$TARGET_CPM)"
if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  log "⚠️ 커밋되지 않은 변경이 있습니다. env.json에 변경 파일 수가 기록됩니다."
fi

failed=0
for ((i = 1; i <= REPEAT; i++)); do
  run_once "$i" || failed=$((failed + 1))
done

# ---------------------------------------------------------------------------
# 회차별 요약
# ---------------------------------------------------------------------------
echo
echo "=== $NAME 요약 ==="
printf '%-8s %-8s %-8s %-8s %-10s %-10s %-10s %s\n' run bidOk 용량 드롭 p95ms p99ms 정리초 판정
for ((i = 1; i <= REPEAT; i++)); do
  dir="perf/results/$NAME-run$i"
  if [[ -f "$dir/oversell-summary.json" && -f "$dir/verify-result.json" ]]; then
    jq -r -s --arg run "run$i" '
      .[0] as $s | .[1] as $v |
      [$run,
       $s.counts.bidOk, $s.seed.capacity, $s.counts.droppedIterations,
       ($s.bidLatencyMs["p(95)"] * 10 | round / 10),
       ($s.bidLatencyMs["p(99)"] * 10 | round / 10),
       ($v.derived.settledAfterSec // "-"),
       (if $v.passed and ($s.oversold | not) then "✅ 통과" else "❌ 실패" end)]
      | @tsv' "$dir/oversell-summary.json" "$dir/verify-result.json" |
      awk -F'\t' '{printf "%-8s %-8s %-8s %-8s %-10s %-10s %-10s %s\n", $1,$2,$3,$4,$5,$6,$7,$8}'
  else
    printf '%-8s %s\n' "run$i" "결과 파일 없음"
  fi
done

echo
if ((failed == 0)); then
  log "✅ 모든 회차 통과 → perf/results/$NAME-run*"
  exit 0
fi
log "❌ ${failed}개 회차 실패 → perf/results/$NAME-run*"
exit 1
