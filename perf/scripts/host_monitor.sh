#!/usr/bin/env bash
# 측정 세션 동안 호스트 상태를 백그라운드로 기록한다. 측정 스크립트와 별개로 돌기 때문에 측정 방식에 영향을 주지 않는다.
# 회차가 실패했을 때 그 시점에 측정과 무관한 호스트 활동(배경 작업)이나 발열이 있었는지 확인하는 데 쓴다.
#
#   perf/scripts/host_monitor.sh start <디렉토리>   기록 시작 (이미 돌고 있으면 그대로 둔다)
#   perf/scripts/host_monitor.sh stop               기록 중지
#
# 기록 (<디렉토리>에 추가로 쌓인다)
#   thermal.tsv   epoch, macOS 발열 상태(0 정상 ~ 3 위험), 저전력 모드(0/1). 10초 간격
#   top.txt       호스트 전체 CPU 사용률과 CPU 상위 8개 프로세스. 30초 간격
#                 (com.apple.Virtualization.VirtualMachine = Docker VM, k6 = 부하 발생기, 그 밖은 측정과 무관한 활동)

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
cd "$ROOT_DIR"

PID_FILE="perf/.state/host-monitor.pid"

case "${1:-}" in
  start)
    dir="${2:?사용법: $0 start <디렉토리>}"
    if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
      echo "이미 기록 중 (PID $(cat "$PID_FILE"))" >&2
      exit 0
    fi
    mkdir -p "$dir" perf/.state
    [[ -f "$dir/thermal.tsv" ]] || printf 'epoch\tthermal_state\tlow_power_mode\n' >"$dir/thermal.tsv"
    (
      trap 'kill 0' TERM
      while :; do
        printf '%s\t%s\t%s\n' "$(date +%s)" "$(thermal_state)" \
          "$(pmset -g 2>/dev/null | awk '/lowpowermode/ { print $2 }')" >>"$dir/thermal.tsv"
        sleep 10
      done &
      while :; do
        {
          echo "=== $(date +%s) $(date '+%F %T')"
          top -l 2 -s 1 -n 8 -o cpu -stats pid,command,cpu | awk '/^Processes/ { n++ } n == 2' | grep -E 'CPU usage|^[0-9]'
        } >>"$dir/top.txt" 2>&1
        sleep 29
      done &
      wait
    ) </dev/null >/dev/null 2>&1 &
    echo $! >"$PID_FILE"
    echo "호스트 기록 시작 → $dir (PID $!)" >&2
    ;;
  stop)
    if [[ -f "$PID_FILE" ]]; then
      kill "$(cat "$PID_FILE")" 2>/dev/null || true
      rm -f "$PID_FILE"
      echo "호스트 기록 중지" >&2
    fi
    ;;
  *)
    echo "사용법: $0 start <디렉토리> | stop" >&2
    exit 2
    ;;
esac
