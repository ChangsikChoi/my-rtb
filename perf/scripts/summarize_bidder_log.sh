#!/usr/bin/env bash
# bidder 로그에서 레벨별 건수와 WARN/ERROR 줄을 뽑아 요약 파일로 남긴다.
# 사용: perf/scripts/summarize_bidder_log.sh <bidder.log> > bidder-log-summary.txt

set -euo pipefail

log_file="${1:?bidder.log 경로를 지정하세요}"

echo "lines: $(wc -l <"$log_file" | tr -d ' ')"
echo "levels:"
# logback 패턴: "yyyy-MM-dd HH:mm:ss [thread] LEVEL logger - message"
awk '$4 ~ /^(TRACE|DEBUG|INFO|WARN|ERROR)$/ { count[$4]++ }
     END { for (level in count) printf "  %s: %d\n", level, count[level] }' "$log_file" | sort
echo "warn/error lines:"
grep -E '^[0-9-]+ [0-9:]+ \[[^]]*\] (WARN|ERROR) ' "$log_file" | sed 's/^/  /' || echo "  (없음)"
