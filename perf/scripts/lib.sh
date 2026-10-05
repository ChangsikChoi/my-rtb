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
# 커밋, 호스트, 컨테이너 리소스 제한, 실행 중인 이미지와 측정 파라미터를 env.json으로 남긴다.
#
# env.json의 git.commit은 측정 시점의 HEAD일 뿐, 실행 중인 이미지가 그 코드로 빌드됐다는 보장은 없다.
# 그래서 서비스별 이미지 ID·생성 시각과 앱 디렉토리의 git tree 해시(작업 트리 변경 여부 포함)를 함께 남긴다.
# 이미지 생성 시각이 마지막 앱 코드 변경보다 이전이면 이미지가 오래된 코드로 빌드됐다는 뜻이다.
record_env() {
  local dir="$1" run_label="$2" params="$3"
  local limits='{}' images='{}' sources='{}'
  local svc id image_id
  for svc in bidder redis kafka schema-registry postgres ad-manager log-consumer; do
    id="$("${COMPOSE[@]}" ps -q "$svc")"
    limits="$(jq -c \
      --arg svc "$svc" \
      --argjson nano "$(docker inspect -f '{{.HostConfig.NanoCpus}}' "$id")" \
      --argjson mem "$(docker inspect -f '{{.HostConfig.Memory}}' "$id")" \
      --arg java "$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$id" | sed -n 's/^JAVA_TOOL_OPTIONS=//p')" \
      '. + {($svc): {cpus: ($nano / 1e9), memoryMiB: ($mem / 1048576), javaToolOptions: (if $java == "" then null else $java end)}}' \
      <<<"$limits")"

    image_id="$(docker inspect -f '{{.Image}}' "$id")"
    images="$(jq -c \
      --arg svc "$svc" \
      --arg ref "$(docker inspect -f '{{.Config.Image}}' "$id")" \
      --arg imageId "$image_id" \
      --arg created "$(docker image inspect -f '{{.Created}}' "$image_id")" \
      '. + {($svc): {ref: $ref, id: $imageId, created: $created}}' \
      <<<"$images")"
  done

  # 직접 빌드하는 앱 서비스의 소스 상태 (compose 서비스명 → 디렉토리)
  local app path
  for app in bidder:bidder ad-manager:ad_manager log-consumer:log-consumer; do
    svc="${app%%:*}"
    path="${app#*:}"
    sources="$(jq -c \
      --arg svc "$svc" \
      --arg path "$path" \
      --arg tree "$(git -C "$ROOT_DIR" rev-parse "HEAD:$path")" \
      --arg lastCommit "$(git -C "$ROOT_DIR" log -1 --format='%h %cI' -- "$path")" \
      --argjson dirty "$([[ -n "$(git -C "$ROOT_DIR" status --porcelain -- "$path")" ]] && echo true || echo false)" \
      '. + {($svc): {path: $path, headTree: $tree, lastCommit: $lastCommit, workingTreeDirty: $dirty}}' \
      <<<"$sources")"
  done

  jq -n \
    --arg startedAt "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --arg run "$run_label" \
    --arg commit "$(git -C "$ROOT_DIR" rev-parse HEAD)" \
    --argjson trackedChanges "$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no | grep -c . || true)" \
    --arg hostCpu "$(sysctl -n machdep.cpu.brand_string 2>/dev/null || grep -m1 'model name' /proc/cpuinfo | cut -d: -f2- || echo unknown)" \
    --arg hostCores "$(sysctl -n hw.ncpu 2>/dev/null || nproc)" \
    --arg dockerVm "$(docker info --format '{{.NCPU}} CPU / {{.MemTotal}} bytes')" \
    --arg lowPowerMode "$(pmset -g 2>/dev/null | awk '/lowpowermode/ { print $2 }')" \
    --arg k6 "$(k6 version | head -1)" \
    --argjson limits "$limits" \
    --argjson images "$images" \
    --argjson sources "$sources" \
    --argjson rebuilt "$([[ "$BUILD" == "1" ]] && echo true || echo false)" \
    --argjson params "$params" \
    '{
      startedAt: $startedAt,
      run: $run,
      git: {commit: $commit, trackedChanges: $trackedChanges},
      host: {cpu: $hostCpu, cores: $hostCores, dockerVm: $dockerVm, k6: $k6,
             lowPowerMode: (if $lowPowerMode == "" then null else ($lowPowerMode == "1") end)},
      containerLimits: $limits,
      images: $images,
      imagesRebuiltThisRun: $rebuilt,
      appSources: $sources,
      params: $params
    }' >"$dir/env.json"
}

redis_cli() {
  "${COMPOSE[@]}" exec -T redis redis-cli "$@" | tr -d '\r'
}

# macOS 발열 상태 (NSProcessInfo.thermalState: 0 정상, 1 약간 높음, 2 심각, 3 위험). macOS가 아니면 0.
thermal_state() {
  osascript -l JavaScript -e 'ObjC.import("Foundation"); $.NSProcessInfo.processInfo.thermalState' 2>/dev/null || echo 0
}

# bidder JVM(PID 1)의 스레드별 누적 CPU tick을 "tid|이름|tick" 형식으로 남긴다.
bidder_thread_snapshot() {
  "${COMPOSE[@]}" exec -T bidder sh -c '
    for t in /proc/1/task/*; do
      name=$(cat "$t/comm" 2>/dev/null) || continue
      ticks=$(cut -d")" -f2- "$t/stat" 2>/dev/null | awk "{print \$12 + \$13}") || continue
      echo "${t##*/}|$name|$ticks"
    done' | tr -d '\r' >"$1"
}

# bidder_thread_cores <이전 스냅샷> <이후 스냅샷> <초> <CLK_TCK>
# 출력: "가장 바쁜 Lettuce 스레드 코어<TAB>두 스냅샷에 모두 있는 스레드의 합계 코어"
bidder_thread_cores() {
  awk -F'|' -v secs="$3" -v hz="$4" '
    NR == FNR { base[$1] = $3; next }
    ($1 in base) {
      d = $3 - base[$1]; if (d <= 0) next
      total += d
      if ($2 ~ /^lettuce/ && d > top) top = d
    }
    END { printf "%.3f\t%.3f\n", top / hz / secs, total / hz / secs }' "$1" "$2"
}

bidder_clk_tck() {
  local v
  v="$("${COMPOSE[@]}" exec -T bidder getconf CLK_TCK 2>/dev/null | tr -d '\r' || true)"
  [[ "$v" =~ ^[0-9]+$ ]] && echo "$v" || echo 100
}
