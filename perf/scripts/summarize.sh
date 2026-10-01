#!/usr/bin/env bash
# 측정 결과 디렉토리들을 조건별로 묶어 반복 회차의 중앙값 표(Markdown)를 만든다.
#
# perf/results/<조건>-runN 디렉토리에서 "-runN"을 뗀 이름을 하나의 조건으로 묶는다.
# 시나리오는 파일로 판별한다: summary.json → 시나리오 B(처리 용량·병목, latency), oversell-summary.json → 시나리오 A(동시성 정합성, oversell)
#
# 사용 예:
#   perf/scripts/summarize.sh baseline-                       # 표준 출력
#   perf/scripts/summarize.sh -o perf/results/BASELINE.md baseline- oversell-
#   perf/scripts/summarize.sh -t "캐시 적용 후" -o perf/results/CACHE.md cache-
#   perf/scripts/summarize.sh -t "기준선" -n perf/results/BASELINE.notes.md -o perf/results/BASELINE.md baseline- oversell-
#
# 인자: 결과 디렉토리 이름 접두사 (여러 개 가능)
# 옵션: -o 출력 파일, -t 문서 제목 (기본 "성능 측정 요약"),
#       -n 해석 노트 파일 (Markdown, 제목 아래·표 위에 그대로 넣는다. 표를 다시 생성해도 노트는 유지된다)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RESULTS_DIR="$ROOT_DIR/perf/results"

output=""
title="성능 측정 요약"
notes=""
while getopts "o:t:n:" opt; do
  case "$opt" in
    o) output="$OPTARG" ;;
    t) title="$OPTARG" ;;
    n) notes="$OPTARG" ;;
    *) exit 2 ;;
  esac
done
shift $((OPTIND - 1))
[[ $# -gt 0 ]] || {
  echo "사용법: $0 [-o 출력파일] [-t 제목] <접두사>..." >&2
  exit 2
}
command -v jq >/dev/null || { echo "jq가 필요합니다" >&2; exit 2; }
[[ -z "$notes" || -f "$notes" ]] || { echo "노트 파일이 없습니다: $notes" >&2; exit 2; }

# 공통 jq 함수: 중앙값, 반올림
JQ_LIB='
  def med: map(select(. != null)) | sort |
    if length == 0 then null
    elif length % 2 == 1 then .[length / 2 | floor]
    else (.[length / 2 - 1] + .[length / 2]) / 2 end;
  def r(n): if . == null then null else (. * pow(10; n) | round / pow(10; n)) end;
  def fmt: if . == null then "-" else tostring end;
  # Linux 스레드 이름은 15자로 잘리므로 알려진 풀 이름으로 되돌린다.
  def thread_name: {"lettuce-nioEven": "lettuce-nioEventLoop", "reactor-http-ni": "reactor-http-nio",
    "kafka-producer-": "kafka-producer-network", "C2 CompilerThre": "C2 CompilerThread"}[.] // .;
  def range_str(xs): (xs | map(select(. != null))) as $v |
    if ($v | length) < 2 then "" else " (\($v | min | r(1))~\($v | max | r(1)))" end;
'

# ---------------------------------------------------------------------------
# 대상 조건 수집
# ---------------------------------------------------------------------------
groups=()
for prefix in "$@"; do
  for dir in "$RESULTS_DIR/$prefix"*/; do
    [[ -d "$dir" ]] || continue
    name="$(basename "$dir")"
    group="${name%-run[0-9]*}"
    [[ " ${groups[*]-} " == *" $group "* ]] || groups+=("$group")
  done
done
[[ ${#groups[@]} -gt 0 ]] || { echo "접두사에 해당하는 결과가 없습니다: $*" >&2; exit 1; }
IFS=$'\n' read -r -d '' -a groups < <(printf '%s\n' "${groups[@]}" | sort -V && printf '\0')

# 조건에 속한 회차 디렉토리 목록 (회차 번호순)
run_dirs() {
  local group="$1" d
  for d in "$RESULTS_DIR/$group" "$RESULTS_DIR/$group"-run*; do
    [[ -d "$d" ]] && echo "$d"
  done | sort -V
}

# 회차별 {run, summary, env} 배열
collect_runs() {
  local file="$1" group="$2" d
  while read -r d; do
    [[ -f "$d/$file" ]] || continue
    jq -n \
      --arg run "$(basename "$d")" \
      --slurpfile s "$d/$file" \
      --argjson e "$(cat "$d/env.json" 2>/dev/null || echo null)" \
      --argjson v "$(cat "$d/verify-result.json" 2>/dev/null || echo null)" \
      '{run: $run, summary: $s[0], env: $e, verify: $v}'
  done < <(run_dirs "$group") | jq -s .
}

# ---------------------------------------------------------------------------
# 렌더링
# ---------------------------------------------------------------------------
render_latency() {
  local group="$1" runs="$2"
  jq -r "$JQ_LIB"'
    . as $runs |
    ($runs | map(.summary.steps[].rate) | unique) as $rates |
    ($runs[0].summary.campaigns) as $campaigns |
    ($runs | map(.summary.maxSustainableRps)) as $max |
    # 모든 회차가 측정한 최고 단계까지 SLA를 통과했다면 한계에 도달하지 못한 것이다.
    ($runs | all(.summary.steps | all(.slaOk))) as $notReached |
    "### \($campaigns)개 캠페인 (`'"$group"'`, \($runs | length)회)\n",
    "- **p99 ≤ \($runs[0].summary.slaP99Ms)ms 최대 처리량: \($max | med | fmt)\(if $notReached then " 이상" else "" end) req/s** (회차별: \($max | map(fmt) | join(", ")))\(if $notReached then " — 측정한 최고 단계까지 SLA 통과, 한계 미도달" else "" end)",
    # 모든 회차가 SLA를 통과한 가장 높은 요청률 (보수적인 기준선 값)
    ([$rates[] as $rate | select([$runs[].summary.steps[] | select(.rate == $rate)] |
        length == ($runs | length) and all(.slaOk)) | $rate] | max) as $allPass |
    (if ($runs | length) > 1 then
      "- **모든 회차(\($runs | length)/\($runs | length)) 통과 최대 처리량: \($allPass | fmt) req/s** — 기준선 비교에는 이 값을 쓴다"
     else empty end),
    "- 표의 값은 회차별 중앙값이고, p99 괄호는 회차 간 최소~최대다.\n",
    "| 요청률 | 실제 처리 | p50 ms | p95 ms | p99 ms | SLA 통과 | bidder CPU % | 최다 스레드 코어 | Redis 명령/요청 | HGETALL/요청 | Redis 코어 |",
    "|---:|---:|---:|---:|---:|:---:|---:|---|---:|---:|---:|",
    ($rates[] as $rate |
      [$runs[].summary.steps[] | select(.rate == $rate)] as $s |
      ($s | map(.bidder.busiestThread.cores // null) | med | r(2)) as $busy |
      ($s | map(.bidder.busiestThread.group // null) | map(select(. != null) | thread_name) | unique | join("/")) as $busyName |
      "| \($rate) | \($s | map(.achievedRps) | med | r(1) | fmt)" +
      " | \($s | map(.bidLatencyMs["p(50)"]) | med | r(1) | fmt)" +
      " | \($s | map(.bidLatencyMs["p(95)"]) | med | r(1) | fmt)" +
      " | \($s | map(.bidLatencyMs["p(99)"]) | med | r(1) | fmt)\(range_str($s | map(.bidLatencyMs["p(99)"])))" +
      " | \($s | map(select(.slaOk)) | length)/\($s | length)\(if ($s | map(select(.saturated)) | length) > 0 then " (포화 \($s | map(select(.saturated)) | length))" else "" end)" +
      " | \($s | map(.bidder.cpu.avgPercent) | med | r(0) | fmt)" +
      " | \(if $busy == null then "-" else "\($busyName) \($busy)" end)" +
      " | \($s | map(.redis.callsPerBid) | med | r(1) | fmt)" +
      " | \($s | map(.redis.hgetallPerBid) | med | r(1) | fmt)" +
      " | \($s | map(.redis.cpuCoresUsed) | med | r(2) | fmt) |"),
    ""
  ' <<<"$runs"
}

render_oversell() {
  local group="$1" runs="$2"
  jq -r "$JQ_LIB"'
    . as $runs |
    ($runs[0].summary) as $f |
    ($runs | map(select(.summary.oversold == false and (.verify.passed // false))) | length) as $pass |
    "### 예산 정합성: \($f.config.rate) req/s × \($f.config.durationSec)s, 용량 \($f.seed.capacity)회 (`'"$group"'`, \($runs | length)회)\n",
    "- **판정: \($pass)/\($runs | length) 통과**\n",
    "| 회차 | 성공 입찰 / 용량 | 확정 / win 호출 | 남은 예약 키 | 최종 잔액 일치 | 드롭 | p95 ms | p99 ms | 정리 초 | 판정 |",
    "|---|---:|---:|---:|:---:|---:|---:|---:|---:|:---:|",
    ($runs[] |
      (.verify.checks // []) as $c |
      ($c | map(select(.name | test("reservation"))) | map(.actual | tonumber) | add) as $keys |
      ($c | map(select(.name | test("budget_total ==")))[0].passed) as $balance |
      "| \(.run) | \(.summary.counts.bidOk) / \(.summary.seed.capacity)" +
      " | \(.verify.derived.confirmed | fmt) / \(.summary.counts.winSent)" +
      " | \($keys | fmt)" +
      " | \(if $balance == null then "-" elif $balance then "✅" else "❌" end)" +
      " | \(.summary.counts.droppedIterations)" +
      " | \(.summary.bidLatencyMs["p(95)"] | r(1) | fmt) | \(.summary.bidLatencyMs["p(99)"] | r(1) | fmt)" +
      " | \(if .summary.note then "-" else (.verify.derived.settledAfterSec | fmt) end)" +
      " | \(if .summary.oversold == false and (.verify.passed // false) then "✅" else "❌" end) |"),
    ($runs | map(select(.summary.note)) | if length > 0 then "", (.[] | "- 비고 `\(.run)`: \(.summary.note)") else empty end),
    ""
  ' <<<"$runs"
}

# 측정 환경: 회차들이 같은 이미지·같은 제한에서 나왔는지 확인한다.
render_env() {
  local all="$1"
  jq -r '
    map(select(.env != null)) as $e |
    ($e | map(.env.images.bidder.id // null) | map(select(. != null) | ltrimstr("sha256:") | .[0:12]) | unique) as $imgs |
    ($e | map(select(.env.images.bidder.id == null)) | length) as $noImg |
    ($e | map(.env.git.commit[0:7]) | unique) as $commits |
    ($e | map(.env.containerLimits.bidder | "\(.cpus) vCPU / \(.memoryMiB) MiB / \(.javaToolOptions)") | unique) as $limits |
    ($e | map(.env.host | "\(.cpu), \(.cores)코어, Docker VM \(.dockerVm)") | unique) as $hosts |
    ($e | map(.env.imagesRebuiltThisRun // false) | any) as $rebuilt |
    "## 측정 환경\n",
    "| 항목 | 값 |",
    "|---|---|",
    "| 회차 수 | \(length) (env.json 있음 \($e | length)) |",
    "| bidder 이미지 | \(if ($imgs | length) == 0 then "기록 없음" else ($imgs | join(", ")) end)\(if ($imgs | length) > 1 then " ⚠️ 여러 이미지 혼재" else "" end)\(if $noImg > 0 and ($imgs | length) > 0 then " (이미지 기록 이전 회차 \($noImg)개 제외)" else "" end) |",
    "| 측정 시점 커밋 | \($commits | join(", ")) |",
    "| bidder 리소스 | \($limits | join(" · "))\(if ($limits | length) > 1 then " ⚠️ 조건 혼재" else "" end) |",
    "| 호스트 | \($hosts | join(" · ")) |",
    "| 측정 중 이미지 재빌드 | \(if $rebuilt then "있음" else "없음" end) |",
    ""
  ' <<<"$all"
}

# ---------------------------------------------------------------------------
# 문서 생성
# ---------------------------------------------------------------------------
generate() {
  local latency_md="" oversell_md="" all_runs="[]" group runs

  for group in "${groups[@]}"; do
    runs="$(collect_runs summary.json "$group")"
    if [[ "$(jq length <<<"$runs")" -gt 0 ]]; then
      latency_md+="$(render_latency "$group" "$runs")"$'\n\n'
      all_runs="$(jq -c --argjson r "$runs" '. + $r' <<<"$all_runs")"
      continue
    fi
    runs="$(collect_runs oversell-summary.json "$group")"
    if [[ "$(jq length <<<"$runs")" -gt 0 ]]; then
      oversell_md+="$(render_oversell "$group" "$runs")"$'\n\n'
      all_runs="$(jq -c --argjson r "$runs" '. + $r' <<<"$all_runs")"
    fi
  done

  echo "# $title"
  echo
  echo "> \`perf/scripts/summarize.sh $*\`로 생성 ($(date '+%Y-%m-%d %H:%M'), 커밋 $(git -C "$ROOT_DIR" rev-parse --short HEAD))\
${notes:+ · 해석 노트: \`${notes#"$ROOT_DIR"/}\`}"
  echo
  if [[ -n "$notes" ]]; then
    cat "$notes"
    echo
    echo "---"
    echo
    echo "# 측정 결과 표"
    echo
  fi
  render_env "$all_runs"
  if [[ -n "$latency_md" ]]; then
    echo "## 시나리오 B: 처리 용량·병목 측정"
    echo
    printf '%s' "$latency_md"
  fi
  if [[ -n "$oversell_md" ]]; then
    echo "## 시나리오 A: 동시성 정합성 검증"
    echo
    printf '%s' "$oversell_md"
  fi
}

if [[ -n "$output" ]]; then
  generate "$@" >"$output"
  echo "요약 저장: ${output#"$ROOT_DIR"/}" >&2
else
  generate "$@"
fi
