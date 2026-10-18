#!/usr/bin/env bash
# oversell 시나리오 이후 Redis 예산 상태가 기대값과 일치하는지 검증한다.
#
# 예약 TTL(30초)이 지나 만료 환불이 끝날 때까지 Redis를 폴링한 뒤 다음을 확인한다.
#   1. 성공 입찰 수 ≤ 예산 용량                      (초과 예약 없음)
#   2. budget_reserved == 0                          (모든 예약이 확정 또는 환불됨)
#   3. 남은 reservation / reservation_backup 키 == 0 (환불 누락 없음)
#   4. budget_total ≥ 0                              (잔액 음수 없음)
#   5. budget_total == 초기 예산 − win 호출 수 × 1회 예약액
# 추가로 log-consumer DB의 이벤트 저장 건수를 참고 정보로 비교한다(Kafka 발행이 비동기라 판정에는 넣지 않음).
#
# 사용 예:
#   RESULT_DIR=perf/results/oversell-run1 perf/scripts/verify_budget.sh
#
# 환경 변수:
#   RESULT_DIR         oversell.js를 실행할 때 지정한 결과 디렉토리 (oversell-summary.json을 읽는다)
#   SUMMARY_FILE       요약 파일을 직접 지정할 때 사용 (RESULT_DIR 대신)
#   WAIT_TIMEOUT_SEC   환불 완료를 기다리는 최대 시간 (기본 90)
#   POLL_INTERVAL_SEC  폴링 간격 (기본 2)
#   POSTGRES_USER / POSTGRES_DB  DB 교차 확인용 (기본 docker-compose 값)
#
# 종료 코드: 0 = 모든 판정 통과, 1 = 판정 실패, 2 = 실행 오류

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE=(docker compose -f "$ROOT_DIR/docker-compose.yml")

WAIT_TIMEOUT_SEC="${WAIT_TIMEOUT_SEC:-90}"
POLL_INTERVAL_SEC="${POLL_INTERVAL_SEC:-2}"
POSTGRES_USER="${POSTGRES_USER:-changsik}"
POSTGRES_DB="${POSTGRES_DB:-budgetdb}"

error() {
  echo "[verify] ERROR: $*" >&2
  exit 2
}

command -v jq >/dev/null || error "jq가 필요합니다 (brew install jq)"

if [[ -n "${SUMMARY_FILE:-}" ]]; then
  summary="$SUMMARY_FILE"
elif [[ -n "${RESULT_DIR:-}" ]]; then
  summary="$RESULT_DIR/oversell-summary.json"
else
  error "RESULT_DIR 또는 SUMMARY_FILE을 지정하세요"
fi
[[ -f "$summary" ]] || error "요약 파일이 없습니다: $summary"
out_dir="$(dirname "$summary")"

read_summary() { jq -r "$1" "$summary"; }

campaign_id="$(read_summary '.campaignId')"
budget_micro="$(read_summary '.seed.budgetMicro')"
price_micro="$(read_summary '.seed.impressionPriceMicro')"
capacity="$(read_summary '.seed.capacity')"
bid_ok="$(read_summary '.counts.bidOk')"
bid_error="$(read_summary '.counts.bidError')"
win_sent="$(read_summary '.counts.winSent')"
win_error="$(read_summary '.counts.winError')"
expected_total="$(read_summary '.expected.finalTotalMicro')"

total_key="campaign:${campaign_id}:budget_total"
reserved_key="campaign:${campaign_id}:budget_reserved"

redis() {
  "${COMPOSE[@]}" exec -T redis redis-cli "$@" | tr -d '\r'
}

count_keys() {
  redis --scan --pattern "$1" | grep -c . || true
}

# 요약 파일 수정 시각 = k6 부하 종료 시각
file_mtime() { stat -f %m "$1" 2>/dev/null || stat -c %Y "$1"; }
test_end_epoch="$(file_mtime "$summary")"

redis PING >/dev/null || error "Redis에 연결할 수 없습니다 (docker compose가 실행 중인지 확인)"

# ---------------------------------------------------------------------------
# 만료 환불 대기
# ---------------------------------------------------------------------------
echo "[verify] 캠페인 $campaign_id 의 예약이 모두 확정·환불될 때까지 대기 (최대 ${WAIT_TIMEOUT_SEC}초)"

deadline=$(($(date +%s) + WAIT_TIMEOUT_SEC))
last_report=0
while :; do
  reserved="$(redis GET "$reserved_key")"
  reservations="$(count_keys 'reservation:*')"
  backups="$(count_keys 'reservation_backup:*')"

  if [[ "$reserved" == "0" && "$reservations" == "0" && "$backups" == "0" ]]; then
    settled_after=$(($(date +%s) - test_end_epoch))
    echo "[verify] 예약 정리 완료 (부하 종료 후 약 ${settled_after}초)"
    break
  fi
  if (($(date +%s) >= deadline)); then
    settled_after=null
    echo "[verify] 대기 시간 초과: reserved=${reserved:-<없음>}, reservation 키 ${reservations}개, backup 키 ${backups}개 남음"
    break
  fi
  # 진행 상황은 10초에 한 번만 출력한다
  if (($(date +%s) - last_report >= 10)); then
    printf '[verify]   대기 중: reserved=%s, reservation 키 %s개, backup 키 %s개\n' \
      "${reserved:-<없음>}" "$reservations" "$backups"
    last_report=$(date +%s)
  fi
  sleep "$POLL_INTERVAL_SEC"
done

total="$(redis GET "$total_key")"
[[ "$total" =~ ^-?[0-9]+$ ]] || error "$total_key 값을 읽을 수 없습니다: '${total}'"
[[ "$reserved" =~ ^-?[0-9]+$ ]] || error "$reserved_key 값을 읽을 수 없습니다: '${reserved}'"

# ---------------------------------------------------------------------------
# 판정
# ---------------------------------------------------------------------------
failures=0
checks_json='[]'

record() {
  local name="$1" passed="$2" actual="$3" expected="$4"
  local mark="✅"
  if [[ "$passed" != "true" ]]; then
    mark="❌"
    failures=$((failures + 1))
  fi
  printf '  %s %s: 실제 %s (기대 %s)\n' "$mark" "$name" "$actual" "$expected"
  checks_json="$(jq -c --arg n "$name" --argjson p "$passed" --arg a "$actual" --arg e "$expected" \
    '. + [{name: $n, passed: $p, actual: $a, expected: $e}]' <<<"$checks_json")"
}

bool() { if "$@"; then echo true; else echo false; fi; }

# 초기 예산 = 잔액(total) + 묶인 예약(reserved) + 확정 차감액
spent=$((budget_micro - total - reserved))
confirmed=$((spent / price_micro))

echo
echo "=== 예산 정합성 검증 ==="
record "성공 입찰 수 ≤ 예산 용량" "$(bool test "$bid_ok" -le "$capacity")" "$bid_ok" "≤ $capacity"
record "budget_reserved == 0" "$(bool test "$reserved" -eq 0)" "$reserved" "0"
record "남은 reservation 키" "$(bool test "$reservations" -eq 0)" "$reservations" "0"
record "남은 reservation_backup 키" "$(bool test "$backups" -eq 0)" "$backups" "0"
record "budget_total ≥ 0" "$(bool test "$total" -ge 0)" "$total" "≥ 0"
record "budget_total == 초기 − win × 예약액" "$(bool test "$total" -eq "$expected_total")" "$total" "$expected_total"

echo
echo "  확정 차감액 ${spent} micro → 확정 ${confirmed}회 (win 호출 ${win_sent}회)"
if ((reserved > 0)); then
  echo "  ⚠️ 환불되지 않은 예약 ${reserved} micro (약 $((reserved / price_micro))회분)가 reserved에 묶여 있습니다: 만료 알림 유실 또는 환불 스크립트 실패를 의심하세요"
fi
if ((spent % price_micro != 0)); then
  echo "  ⚠️ 차감액이 1회 예약액(${price_micro})의 배수가 아닙니다"
fi
if ((confirmed < win_sent)); then
  echo "  ⚠️ 확정이 win 호출보다 적습니다: win 도착 전 예약이 만료됐거나 확정 스크립트가 실패했을 수 있습니다"
elif ((confirmed > win_sent)); then
  echo "  ⚠️ 확정이 win 호출보다 많습니다: 중복 차감 가능성이 있습니다"
fi
if ((bid_error > 0 || win_error > 0)); then
  echo "  ⚠️ 부하 중 오류 응답이 있었습니다 (입찰 ${bid_error}, win ${win_error})"
fi

# ---------------------------------------------------------------------------
# 참고: log-consumer DB 저장 건수 (판정 제외)
# ---------------------------------------------------------------------------
db_count() {
  "${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
    "select count(*) from $1 where campaign_id = '$campaign_id'" 2>/dev/null | tr -d '\r[:space:]'
}

bidding_rows="$(db_count bidding_log || true)"
win_rows="$(db_count win_log || true)"

echo
echo "=== 참고: 이벤트 저장 건수 (Kafka 비동기 발행이라 판정 제외) ==="
if [[ "$bidding_rows" =~ ^[0-9]+$ && "$win_rows" =~ ^[0-9]+$ ]]; then
  printf '  bidding_log %-8s (성공 입찰 %s)\n' "$bidding_rows" "$bid_ok"
  printf '  win_log     %-8s (확정 %s)\n' "$win_rows" "$confirmed"
else
  bidding_rows=null
  win_rows=null
  echo "  DB를 조회할 수 없어 건너뜁니다"
fi

# ---------------------------------------------------------------------------
# 결과 저장
# ---------------------------------------------------------------------------
result_file="$out_dir/verify-result.json"
jq -n \
  --arg campaignId "$campaign_id" \
  --argjson passed "$(bool test "$failures" -eq 0)" \
  --argjson checks "$checks_json" \
  --argjson budgetMicro "$budget_micro" \
  --argjson totalMicro "$total" \
  --argjson reservedMicro "$reserved" \
  --argjson confirmed "$confirmed" \
  --argjson winSent "$win_sent" \
  --argjson bidOk "$bid_ok" \
  --argjson settledAfterSec "$settled_after" \
  --argjson biddingLogRows "$bidding_rows" \
  --argjson winLogRows "$win_rows" \
  '{
    campaignId: $campaignId,
    passed: $passed,
    checks: $checks,
    redis: {budgetMicro: $budgetMicro, totalMicro: $totalMicro, reservedMicro: $reservedMicro},
    derived: {confirmed: $confirmed, winSent: $winSent, bidOk: $bidOk, settledAfterSec: $settledAfterSec},
    eventRows: {biddingLog: $biddingLogRows, winLog: $winLogRows}
  }' >"$result_file"

echo
if ((failures == 0)); then
  echo "[verify] ✅ 모든 판정 통과 → ${result_file#"$ROOT_DIR"/}"
  exit 0
fi
echo "[verify] ❌ 판정 실패 ${failures}건 → ${result_file#"$ROOT_DIR"/}"
exit 1
