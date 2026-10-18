#!/usr/bin/env bash
# 성능 측정용 캠페인을 ad_manager API로 생성·활성화하고, 결과를 perf/.state/seed.json에 기록한다.
#
# 사용 예:
#   CAMPAIGNS=1 BUDGET=100 TARGET_CPM=1000 perf/scripts/seed.sh      # oversell 시나리오 (노출 100회분 예산)
#   CAMPAIGNS=100 BUDGET=100000000 perf/scripts/seed.sh              # latency 시나리오 (예산 소진 없음)
#
# 환경 변수:
#   CAMPAIGNS       생성할 캠페인 수 (기본 1)
#   BUDGET          캠페인당 예산, 정수 원 단위 (기본 100)
#   TARGET_CPM      목표 CPM, 정수 원 단위 (기본 1000 → 1회 노출 예약액 1원)
#   AD_MANAGER_URL  ad_manager 주소 (기본 http://localhost:8088)
#   START_DATE      집행 시작일 (기본: 어제. 컨테이너 UTC와 호스트 시간대 차이로 오늘 날짜가 아직 시작 전이 되는 것을 피한다)
#   END_DATE        집행 종료일 (기본 2099-12-31)
#   ALLOW_EXISTING  1이면 Redis에 이미 활성 캠페인이 있어도 진행 (기본 0)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
STATE_DIR="$ROOT_DIR/perf/.state"
SEED_FILE="$STATE_DIR/seed.json"

CAMPAIGNS="${CAMPAIGNS:-1}"
BUDGET="${BUDGET:-100}"
TARGET_CPM="${TARGET_CPM:-1000}"
AD_MANAGER_URL="${AD_MANAGER_URL:-http://localhost:8088}"
END_DATE="${END_DATE:-2099-12-31}"
ALLOW_EXISTING="${ALLOW_EXISTING:-0}"

if [[ -z "${START_DATE:-}" ]]; then
  # macOS(BSD date)와 Linux(GNU date) 모두 지원
  START_DATE="$(date -v-1d +%F 2>/dev/null || date -d yesterday +%F)"
fi

fail() {
  echo "[seed] ERROR: $*" >&2
  exit 1
}

command -v jq >/dev/null || fail "jq가 필요합니다 (brew install jq)"
command -v curl >/dev/null || fail "curl이 필요합니다"

for var in CAMPAIGNS BUDGET TARGET_CPM; do
  [[ "${!var}" =~ ^[1-9][0-9]*$ ]] || fail "$var는 양의 정수여야 합니다: ${!var}"
done

# bidder와 동일한 계산: budgetMicro = budget * 1e6, 1회 노출 예약액 = targetCpmMicro / 1000
BUDGET_MICRO=$((BUDGET * 1000000))
PRICE_MICRO=$((TARGET_CPM * 1000000 / 1000))
CAPACITY=$((BUDGET_MICRO / PRICE_MICRO))

# 다른 활성 캠페인이 남아 있으면 입찰 경쟁에 섞여 측정 결과가 오염된다.
existing="$(docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T redis redis-cli SCARD campaign:ids | tr -d '\r')"
[[ "$existing" =~ ^[0-9]+$ ]] || fail "Redis 상태를 확인할 수 없습니다 (docker compose가 실행 중인지 확인): $existing"
if [[ "$existing" -gt 0 && "$ALLOW_EXISTING" != "1" ]]; then
  fail "Redis에 활성 캠페인이 ${existing}개 남아 있습니다. 'docker compose down -v' 후 다시 올리거나 ALLOW_EXISTING=1로 실행하세요."
fi

curl -sS -o /dev/null "$AD_MANAGER_URL/api/campaigns" -X OPTIONS \
  || fail "ad_manager($AD_MANAGER_URL)에 연결할 수 없습니다"

# 요청 본문과 응답을 임시 파일로 주고받는다.
tmp_body="$(mktemp)"
trap 'rm -f "$tmp_body"' EXIT

request() {
  local method="$1" url="$2" data="${3:-}"
  local status
  if [[ -n "$data" ]]; then
    status="$(curl -sS -o "$tmp_body" -w '%{http_code}' -X "$method" "$url" \
      -H 'Content-Type: application/json' -d "$data")"
  else
    status="$(curl -sS -o "$tmp_body" -w '%{http_code}' -X "$method" "$url")"
  fi
  [[ "$status" =~ ^2 ]] || fail "$method $url → HTTP $status: $(cat "$tmp_body")"
}

run_id="perf-$(date +%Y%m%d%H%M%S)"
ids=()

echo "[seed] 캠페인 ${CAMPAIGNS}개 생성 (예산 ${BUDGET}, CPM ${TARGET_CPM}, 기간 ${START_DATE}~${END_DATE})"

for ((i = 1; i <= CAMPAIGNS; i++)); do
  body="$(jq -n \
    --arg name "$run_id-$i" \
    --argjson cpm "$TARGET_CPM" \
    --argjson budget "$BUDGET" \
    --arg start "$START_DATE" \
    --arg end "$END_DATE" \
    '{
      name: $name,
      targetCpm: $cpm,
      budget: $budget,
      startDate: $start,
      endDate: $end,
      # k6 입찰 요청(Android, KR, 30세)과 일치하는 타겟.
      # 타겟 필드에 null이 있으면 ad_manager 활성화가 실패하므로(CampaignRedisHashMapper) 모두 채운다.
      target: {os: "Android", country: "KR", minAge: 20, maxAge: 40},
      creative: {
        name: ($name + "-creative"),
        imageUrl: "https://example.com/ad.png",
        clickUrl: "https://example.com",
        width: 300,
        height: 250
      }
    }')"

  request POST "$AD_MANAGER_URL/api/campaigns" "$body"
  id="$(jq -r '.id // empty' "$tmp_body")"
  [[ -n "$id" ]] || fail "생성 응답에 id가 없습니다: $(cat "$tmp_body")"

  request PATCH "$AD_MANAGER_URL/api/campaigns/$id/activate"
  ids+=("$id")

  if ((i % 100 == 0 || i == CAMPAIGNS)); then
    echo "[seed] $i/$CAMPAIGNS 완료"
  fi
done

mkdir -p "$STATE_DIR"
printf '%s\n' "${ids[@]}" | jq -R . | jq -s \
  --arg runId "$run_id" \
  --arg adManagerUrl "$AD_MANAGER_URL" \
  --argjson targetCpm "$TARGET_CPM" \
  --argjson budget "$BUDGET" \
  --argjson budgetMicro "$BUDGET_MICRO" \
  --argjson priceMicro "$PRICE_MICRO" \
  --argjson capacity "$CAPACITY" \
  '{
    runId: $runId,
    adManagerUrl: $adManagerUrl,
    campaigns: .,
    targetCpm: $targetCpm,
    budget: $budget,
    budgetMicro: $budgetMicro,
    impressionPriceMicro: $priceMicro,
    capacityPerCampaign: $capacity
  }' >"$SEED_FILE"

echo "[seed] 캠페인당 예산 ${BUDGET_MICRO} micro, 1회 예약액 ${PRICE_MICRO} micro → 캠페인당 최대 ${CAPACITY}회 입찰 가능"
echo "[seed] 결과 저장: ${SEED_FILE#"$ROOT_DIR"/}"
