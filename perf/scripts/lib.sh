#!/usr/bin/env bash
# 성능 측정 실행 스크립트(run_*.sh)가 공통으로 쓰는 함수 모음. source해서 사용한다.
#
# 제공: ROOT_DIR, COMPOSE, log, error, reset_stack, wait_ready, record_env
# 호출 측에서 설정하는 변수: BUILD(1이면 재빌드), READY_TIMEOUT(초)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml" -f "$ROOT_DIR/perf/docker-compose.perf.yml")

BUILD="${BUILD:-0}"
READY_TIMEOUT="${READY_TIMEOUT:-180}"

log() { echo "[run] $(date +%H:%M:%S) $*"; }

error() {
  echo "[run] ERROR: $*" >&2
  exit 2
}

require_commands() {
  local cmd
  for cmd in "$@"; do
    command -v "$cmd" >/dev/null || error "$cmd 가 필요합니다"
  done
}

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

# record_env <결과 디렉토리> <회차 표시> <파라미터 JSON>
# 커밋, 호스트, 컨테이너 리소스 제한과 측정 파라미터를 env.json으로 남긴다.
record_env() {
  local dir="$1" run_label="$2" params="$3"
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
    --arg run "$run_label" \
    --arg commit "$(git -C "$ROOT_DIR" rev-parse HEAD)" \
    --argjson trackedChanges "$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no | grep -c . || true)" \
    --arg hostCpu "$(sysctl -n machdep.cpu.brand_string 2>/dev/null || grep -m1 'model name' /proc/cpuinfo | cut -d: -f2- || echo unknown)" \
    --arg hostCores "$(sysctl -n hw.ncpu 2>/dev/null || nproc)" \
    --arg dockerVm "$(docker info --format '{{.NCPU}} CPU / {{.MemTotal}} bytes')" \
    --arg k6 "$(k6 version | head -1)" \
    --argjson limits "$limits" \
    --argjson params "$params" \
    '{
      startedAt: $startedAt,
      run: $run,
      git: {commit: $commit, trackedChanges: $trackedChanges},
      host: {cpu: $hostCpu, cores: $hostCores, dockerVm: $dockerVm, k6: $k6},
      containerLimits: $limits,
      params: $params
    }' >"$dir/env.json"
}
