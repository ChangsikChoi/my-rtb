#!/usr/bin/env bash
# 측정 시작 전 호스트 상태 확인. 팬 없는 노트북처럼 발열·배경 작업으로 속도가 변하는 호스트에서
# "식은 상태, 평소 속도"에서만 측정을 시작하도록 한다.
#
#   perf/scripts/host_gate.sh init         유휴 상태 기준 속도를 5회 재서 perf/.state/host-ref.json에 저장
#   perf/scripts/host_gate.sh wait         통과할 때까지 1분 간격으로 확인, 결과 JSON 한 줄 출력
#
# 통과 조건: macOS 발열 상태 0, 그리고 host_probe.sh의 호스트·VM 값이 모두 기준값의 (1 + GATE_TOLERANCE) 이하
# 저전력 모드 여부가 기준값을 잴 때와 다르면 기다리지 않고 바로 실패한다(모드가 다르면 CPU 속도가 약 2배 달라 비교가 성립하지 않는다).
# 환경 변수: GATE_TOLERANCE (기본 0.05), GATE_MAX_SEC (기본 1800, 넘으면 실패 종료)

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
cd "$ROOT_DIR"

REF_FILE="perf/.state/host-ref.json"
TOLERANCE="${GATE_TOLERANCE:-0.05}"
MAX_SEC="${GATE_MAX_SEC:-1800}"

low_power_mode() {
  [[ "$(pmset -g 2>/dev/null | awk '/lowpowermode/ { print $2 }')" == "1" ]] && echo true || echo false
}

case "${1:-}" in
  init)
    mkdir -p perf/.state
    hosts=(); vms=()
    for i in 1 2 3 4 5; do
      p="$(perf/scripts/host_probe.sh 3)"
      hosts+=("$(jq .hostMs <<<"$p")"); vms+=("$(jq .vmMs <<<"$p")")
      echo "  프로브 $i: $p (발열 상태 $(thermal_state))" >&2
      if ((i < 5)); then sleep 10; fi
    done
    median() { printf '%s\n' "$@" | sort -n | sed -n 3p; }
    jq -n --argjson h "$(median "${hosts[@]}")" --argjson v "$(median "${vms[@]}")" \
      --arg at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" --argjson lpm "$(low_power_mode)" \
      '{hostMs: $h, vmMs: $v, lowPowerMode: $lpm, measuredAt: $at}' | tee "$REF_FILE"
    ;;
  wait)
    [[ -f "$REF_FILE" ]] || error "기준값이 없습니다. 먼저 $0 init"
    ref_host="$(jq .hostMs "$REF_FILE")"; ref_vm="$(jq .vmMs "$REF_FILE")"
    ref_lpm="$(jq -r '.lowPowerMode // "unknown"' "$REF_FILE")"
    if [[ "$ref_lpm" != "unknown" && "$ref_lpm" != "$(low_power_mode)" ]]; then
      echo "  [gate] 저전력 모드가 기준값과 다릅니다 (기준: $ref_lpm, 현재: $(low_power_mode))." \
        "시스템 설정 → 배터리에서 저전력 모드를 기준과 같게 맞춘 뒤 다시 실행하세요." >&2
      jq -cn --arg ref "$ref_lpm" --argjson cur "$(low_power_mode)" \
        '{passed: false, reason: "low power mode mismatch", refLowPowerMode: ($ref == "true"), lowPowerMode: $cur}'
      exit 1
    fi
    start="$(date +%s)"; attempts=0
    while :; do
      attempts=$((attempts + 1))
      thermal="$(thermal_state)"
      p="$(perf/scripts/host_probe.sh 3)"
      host="$(jq .hostMs <<<"$p")"; vm="$(jq .vmMs <<<"$p")"
      ok="$(awk -v t="$thermal" -v h="$host" -v v="$vm" -v rh="$ref_host" -v rv="$ref_vm" -v tol="$TOLERANCE" \
        'BEGIN { print (t == 0 && h <= rh * (1 + tol) && v <= rv * (1 + tol)) ? "true" : "false" }')"
      waited=$(($(date +%s) - start))
      echo "  [gate] $(date +%H:%M:%S) 발열 $thermal, host ${host}ms (기준 $ref_host), vm ${vm}ms (기준 $ref_vm) → $ok" >&2
      if [[ "$ok" == "true" || "$waited" -ge "$MAX_SEC" ]]; then
        jq -cn --argjson passed "$ok" --argjson waited "$waited" --argjson attempts "$attempts" \
          --argjson thermal "$thermal" --argjson host "$host" --argjson vm "$vm" \
          --argjson refHost "$ref_host" --argjson refVm "$ref_vm" --argjson tol "$TOLERANCE" \
          '{passed: $passed, waitedSec: $waited, attempts: $attempts, thermalState: $thermal,
            hostMs: $host, vmMs: $vm, refHostMs: $refHost, refVmMs: $refVm, tolerance: $tol}'
        [[ "$ok" == "true" ]] && exit 0 || exit 1
      fi
      sleep 60
    done
    ;;
  *)
    echo "사용법: $0 init | wait" >&2
    exit 2
    ;;
esac
