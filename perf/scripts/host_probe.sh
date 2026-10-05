#!/usr/bin/env bash
# 호스트 속도 프로브: 고정된 단일 스레드 계산(perl 루프 4천만 회)에 걸린 시간을 잰다.
# 열 스로틀링 등으로 호스트가 느려지면 값이 커진다. 측정 사이 휴지가 충분한지 판단하는 데 쓴다.
#
# 호스트(macOS)와 Docker VM 안(redis:7 컨테이너, 1 CPU) 두 곳에서 각각 N회 재고 최솟값을 남긴다.
# 최솟값을 쓰는 이유: 순간적인 방해(다른 프로세스)는 시간을 늘리기만 하므로 최솟값이 그 시점의 속도에 가장 가깝다.
#
# 사용: perf/scripts/host_probe.sh [N]   (기본 3)
# 출력(JSON 한 줄): {"hostMs": 905, "vmMs": 955}

set -euo pipefail

N="${1:-3}"
LOOP='my $x = 0; for my $i (1..40_000_000) { $x += ($i * 7) % 13 }'

host_ms() {
  perl -MTime::HiRes=time -e "my \$t = time; $LOOP; printf \"%d\\n\", (time - \$t) * 1000"
}

best_host=""
for ((i = 0; i < N; i++)); do
  v="$(host_ms)"
  [[ -z "$best_host" || "$v" -lt "$best_host" ]] && best_host="$v"
done

# VM 안에는 Time::HiRes가 없어 셸의 나노초 시계로 잰다(perl 시작 비용 수 ms 포함).
best_vm="$(docker run --rm --cpus=1 redis:7 sh -c "
  best=''
  for i in \$(seq $N); do
    s=\$(date +%s%N); perl -e '$LOOP'; e=\$(date +%s%N)
    v=\$(( (e - s) / 1000000 ))
    if [ -z \"\$best\" ] || [ \"\$v\" -lt \"\$best\" ]; then best=\$v; fi
  done
  echo \$best" 2>/dev/null || echo null)"

echo "{\"hostMs\": $best_host, \"vmMs\": $best_vm}"
