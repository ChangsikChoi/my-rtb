# perf

`bidder` 성능 측정 도구와 측정 결과입니다. 결과 요약과 해석은 저장소 루트의 [성능 검증](../performance-verification.md) 문서에 있고, 이 문서는 디렉토리 안내입니다.

## 도구

| 경로 | 내용 |
| --- | --- |
| `docker-compose.perf.yml` | 측정용 컨테이너 리소스 제한 (bidder 2 vCPU / 1 GiB, 힙 512 MiB 고정, G1 GC 등) |
| `k6/latency.js`, `oversell.js`, `warmup.js` | k6 부하 스크립트 (요청률 고정 방식) |
| `scripts/run_oversell.sh` | 예산 정합성 측정 (초기화 → 워밍업 → 시드 → 부하 → Redis 검증) |
| `scripts/run_latency.sh` | 처리 용량 측정 (요청률을 단계적으로 올리며 지연·스레드별 CPU·Redis 명령 수집) |
| `scripts/run_latency_cooled.sh` | 처리 용량 3회 반복 측정. 회차마다 시작 전 호스트 상태 확인, 회차 사이 휴지 (팬 없는 노트북용) |
| `scripts/run_sustained.sh` | 지속 부하 측정 (고정 요청률을 10분 유지하며 분 단위 기록) |
| `scripts/host_gate.sh`, `host_probe.sh`, `host_monitor.sh` | 측정 시작 조건 확인, 단일 스레드 속도 프로브, 측정 중 발열 상태 기록 |
| `scripts/thermal_profile.sh` | 발열 특성 측정 (고정 부하 중 성능 변화와 회복 시간) |
| `scripts/summarize.sh` | 결과 디렉토리를 조건별 결과표(Markdown)로 요약 |
| `scripts/seed.sh`, `verify_budget.sh`, `summarize_bidder_log.sh`, `lib.sh` | 캠페인 시드, 예산 검증, 로그 요약, 공통 함수 |

각 스크립트의 사용법과 환경 변수는 파일 상단 주석에 있습니다. 모든 명령은 저장소 루트에서 실행합니다.

## 결과 (`results/`)

모든 결과는 Apple M4 MacBook Air에서 측정했습니다. 디렉토리 이름은 `<측정>-pc2-<조건>-run<회차>` 형식이며, `pc2`는 이 측정 장비를 나타냅니다.

저장소에는 판정과 근거에 필요한 요약 파일만 있습니다. 원본 출력 전체와 기준선으로 쓰지 않은 측정은 [GitHub Release `perf-results-2026-10`](https://github.com/ChangsikChoi/my-rtb/releases/tag/perf-results-2026-10)에 첨부한 `my-rtb-perf-raw-results.zip`에 있습니다. 원본 `bidder.log`는 용량(회차당 수백 MB) 때문에 zip에도 넣지 않았고, 회차별 로그 요약(`bidder-log-summary.txt`)만 있습니다.

### 저장소에 있는 것 (성능 검증 문서의 근거)

| 디렉토리 | 내용 | 회차별 파일 |
| --- | --- | --- |
| `oversell-pc2-r500-run*`, `oversell-pc2-r500-b5000-run*`, `oversell-pc2-r2000-b20000-run*` | 예산 정합성 3개 조건 × 3회 (일반 모드) | `env.json`, `seed.json`, `oversell-summary.json`, `verify-result.json`, `verify-output.txt`, `bidder-log-summary.txt` |
| `baseline-pc2-c10-run*`, `baseline-pc2-c100-run*`, `baseline-pc2-c1000-run*` | 처리 용량 3개 조건 × 3회 (저전력 모드) | `env.json`, `seed.json`, `summary.json`, `summary.tsv`, `bidder-log-summary.txt`, 단계별 `step-summary.json`·`bidder-thread-cpu.tsv`·`redis-commandstats.txt` |
| `sustained-pc2-c10-r*-run1` | 지속 부하 400~750 req/s, 요청률당 1회 (저전력 모드) | `env.json`, `sustained-summary.json`, `sustained-summary.tsv` |
| `hostmon-pc2-lpm/thermal.tsv` | 저전력 모드 측정 동안의 발열 상태 기록 (10초 간격) | |
| `archive-pc2-attempt2/thermal-pc2-c10-r1000/refund-listener-per-minute.tsv` | 일반 모드 20분 지속 측정의 분 단위 입찰·환불·만료 리스너 스레드 수 (원본 로그에서 집계) | |
| `RESULTS.md` | 위 측정의 조건·회차별 결과표 | |
| `BASELINE-pc2.md` (`.notes.md`) | 처리 용량 상세 보고서. 측정 환경(저전력 모드)을 정한 과정과 개선 후 비교 규칙 포함 | |

### 원본 zip에만 있는 것

| 내용 | 위치 (zip 안) |
| --- | --- |
| 위 측정들의 원본 출력: k6 전체 지표·출력, 워밍업 출력, docker stats 샘플, Redis 메모리·키 정보, 지속 부하의 분 단위 k6 요약 | 각 결과 디렉토리 |
| 저전력 모드 측정 동안의 호스트 CPU 상위 프로세스 기록 | `hostmon-pc2-lpm/top.txt` |
| M4 첫 시도(일반 모드). 워밍업 요청률 설정 실수와 연속 측정 중 발열로 무효 처리 | `archive-pc2-attempt1/` |
| M4 두 번째 시도(일반 모드). 측정 도중 발열로 회차 간 편차가 커서 기준선으로 쓰지 않음. 발열 특성 측정(`thermal-pc2-*`), 저전력 모드 시험(`lpm-test-*`) 포함 | `archive-pc2-attempt2/` |

## 작업 노트

[`NEXT_STEPS.md`](NEXT_STEPS.md)는 측정 작업의 인계 노트입니다. 개선 작업 계획, 측정 실행 시 주의 사항, 측정 방법을 정한 이유가 있습니다.
