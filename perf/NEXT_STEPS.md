# 성능 개선 작업 인계 노트

성능 측정 도구를 만들고 개선 전 기준선을 확정한 뒤, 개선 작업을 이어가기 위해 남기는 노트다(2026-10-18 갱신). 성능 검증 결과의 공개용 요약은 저장소 루트의 [`performance-verification.md`](../performance-verification.md)에 있다. 다음 세션은 이 문서와 [`results/BASELINE-pc2.md`](results/BASELINE-pc2.md)(개선 작업의 비교 기준)를 먼저 읽고 시작한다.

## 1. 현재 상태

### 지금까지 한 일

모든 측정은 Apple M4 MacBook Air에서 했다.

- **예산 정합성**: 동시 입찰에서 Redis Lua 예약·확정·환불이 예산을 정확히 지키는지 검증했다. 3개 조건 × 3회 모두 통과(초과 예약 0건, 잔액 일치, 남은 예약 키 0개). 일반 모드에서 측정했다(`oversell-pc2-*`).
- **처리 용량과 병목**: 캠페인 10/100/1,000개에서 p99 ≤ 100ms 처리 용량을 3회씩 측정했다. 기준값은 각각 750 / 200 / 18 req/s(저전력 모드). 병목은 요청마다 전체 캠페인을 조회·매핑하는 작업이 단일 Lettuce I/O 스레드에서 실행되는 것(요청당 Lettuce CPU 0.75 / 4.5 / 47 ms). 캠페인 10개에서는 bidder 전체 CPU가 먼저 한계에 닿는다.
- **만료 환불 경로 문제**: 같은 요청률을 계속 유지하면 환불이 붙는 순간 bidder CPU가 크게 늘고(환불마다 새 스레드), 미환불 예약이 멈추지 않고 늘어난다. 60초 단계 측정에는 거의 드러나지 않아 지속 측정(`run_sustained.sh`)으로 확인했다.
- **과부하·장애 내성**: 미구현. 처리 용량 측정에서 한계를 넘으면 대기열이 쌓여 p50이 수 초로 늘어나는 것을 관찰했다.
- 자세한 결과·해석·측정 조건·비교 규칙은 [`results/BASELINE-pc2.md`](results/BASELINE-pc2.md)(해석 원문 `BASELINE-pc2.notes.md`), 조건·회차별 결과표는 [`results/RESULTS.md`](results/RESULTS.md)에 있다.

### 주요 파일

| 경로 | 내용 |
|---|---|
| `perf/docker-compose.perf.yml` | 측정용 리소스 제한 (bidder 2 vCPU / 1 GiB / 힙 512 MiB 고정 / G1) |
| `perf/scripts/run_oversell.sh` | 예산 정합성 측정 (초기화 → 워밍업 → 시드 → 부하 → Redis 검증) |
| `perf/scripts/run_latency.sh` | 처리 용량 측정 (초기화 → 캠페인 N개 시드 → 워밍업 → 요청률 단계별 측정) |
| `perf/scripts/run_latency_cooled.sh` | 처리 용량 3회 반복. 회차마다 시작 전 확인 → `run_latency.sh` 1회 → 스택 내림, 회차 사이 휴지, 3회 후 이상 회차 검사 |
| `perf/scripts/run_sustained.sh` | 지속 부하 측정: 고정 요청률을 10분 유지하며 분마다 p99·bidder CPU·미환불 예약 기록 (진입 구간 미구현, 4장 참고) |
| `perf/scripts/host_gate.sh` | 시작 전 확인: 발열 상태 0, 단일 스레드 프로브가 유휴 기준값의 5% 이내, 저전력 모드가 기준값과 같음 (`init`으로 기준값 측정) |
| `perf/scripts/host_probe.sh`, `host_monitor.sh` | 단일 스레드 속도 프로브(호스트·Docker VM), 측정 중 발열 상태·CPU 상위 프로세스 기록 |
| `perf/scripts/thermal_profile.sh` | 발열 특성 측정 (고정 부하 유지 중 분 단위 요청당 CPU, 멈춘 뒤 회복 시간) |
| `perf/scripts/summarize.sh` | 결과 디렉토리를 조건별 중앙값 표(Markdown)로 요약 |
| `perf/scripts/lib.sh` | 공통 함수 (스택 초기화, 준비 대기, `env.json` 기록, 발열 상태, bidder 스레드별 CPU) |
| `perf/scripts/seed.sh`, `verify_budget.sh`, `summarize_bidder_log.sh` | 시드, 예산 검증, 로그 요약 |
| `perf/k6/oversell.js`, `latency.js`, `warmup.js` | k6 부하 스크립트 |
| `perf/results/` | 회차별 결과 (`env.json`에 커밋·이미지 ID·리소스 제한·호스트·파라미터 기록). 디렉토리 안내는 [`perf/README.md`](README.md) |

### 브랜치와 로컬 상태

- 측정 도구, 기준선 문서, 이 노트, 결과의 요약 파일은 `main`에 있다. 원본 출력(k6 원본, docker stats, 호스트 기록)과 기준선으로 쓰지 않은 측정(`archive-pc2-attempt*/`)은 원본 zip([GitHub Release `perf-results-2026-10`](https://github.com/ChangsikChoi/my-rtb/releases/tag/perf-results-2026-10)의 `my-rtb-perf-raw-results.zip`)에 있다. 개선 작업 브랜치는 `main`에서 딴다.
- 측정 작업은 앱 코드를 바꾸지 않았다. 기준선은 `main`의 앱 코드(`e86468a`)와 같은 소스로 빌드한 이미지로 측정했다.
- 원본 `bidder.log`는 용량 때문에 git에서 제외했다. 회차별 `bidder-log-summary.txt`만 커밋돼 있다. 만료 환불 분석에 쓴 일반 모드 20분 측정은 원본 로그에서 분 단위 집계표(`archive-pc2-attempt2/thermal-pc2-c10-r1000/refund-listener-per-minute.tsv`)를 만들어 남겼다.

## 2. 측정 환경 준비

### 사전 준비

- 도구: Docker(Compose v2), k6, jq, curl, perl, git. 스크립트는 macOS/Linux 양쪽 명령 차이를 처리하도록 작성했지만 macOS에서만 검증했다.
- Docker 리소스: 측정용 제한의 합계가 CPU 9개, 메모리 약 5.8 GiB다. Docker VM에 **CPU 10개 이상, 메모리 8 GiB 이상**을 권한다. 부족하면 bidder·Redis 제한은 그대로 두고 나머지 서비스만 줄이며, 그 사실을 기준선 문서에 남긴다.
- 포트: 5432, 6379, 8080, 8081, 8088, 9092를 다른 프로세스·컨테이너가 쓰고 있지 않아야 한다.
- 이미지는 첫 실행에서 `BUILD=1`로 빌드한다(3개 서비스 Gradle 빌드에 수 분).

### 측정 장비 조건 (팬 없는 노트북)

- **처리 용량·지속 부하는 저전력 모드에서 측정한다.** 일반 모드에서는 측정 도중 발열 상태가 1로 오르고 1~2분 뒤부터 같은 일에 CPU를 20~25% 더 써서 회차마다 결과가 흔들렸다. 저전력 모드에서는 측정 내내 발열 상태 0, 회차 간 편차 ±1~2.5%였다(절대값은 일반 모드의 약 절반).
- 세션을 처음 시작할 때 유휴 상태 기준값을 잰다: `perf/scripts/host_gate.sh init`. 기준값에는 저전력 모드 여부도 저장되며, 측정 시작 시 모드가 다르면 `host_gate.sh`가 측정을 시작하지 않는다.
- 전원 어댑터를 연결하고, 브라우저 등 다른 앱은 닫는다. 측정 중에는 `host_monitor.sh`로 호스트 상태를 기록한다.
- 측정 명령과 절차는 `BASELINE-pc2.md`의 "개선 결과와 비교할 때의 규칙"을 따른다.

## 3. 측정 실행 가이드

### 반드시 지킬 것

- **레포 루트에서 실행한다.** 다른 디렉토리에서는 스크립트를 찾지 못한다.
- **측정을 동시에 두 개 돌리지 않는다.** 각 회차가 시작할 때 스택을 `down -v`로 초기화하므로 실행 중인 측정이 망가진다. 실행 중인지는 `ps -eo command | grep -E 'run_latency|run_sustained|run_oversell'`로 확인한다.
- **실행 중인 스크립트 파일을 수정하지 않는다.** bash는 스크립트를 실행하면서 읽어 들이므로 중간에 고치면 오동작한다.
- **측정 중에는 호스트에서 무거운 작업을 하지 않는다.** k6가 같은 호스트에서 돌기 때문에 결과에 섞인다.

### 겪었던 문제와 해결

| 증상 | 원인 | 해결 |
|---|---|---|
| `Bind for 0.0.0.0:5432 failed: port is already allocated` | 다른 Docker 스택이 같은 포트 사용 | 그 스택을 내린 뒤 `docker compose down -v`로 정리하고 다시 기동 |
| 앱이 `UnknownHostException: postgres`로 종료 | 포트 충돌로 실패한 기동이 네트워크 등록이 덜 된 컨테이너를 남김 | `docker compose down -v` 후 다시 기동 |
| seed가 "활성 캠페인 N개 남음"으로 중단 | 이전 상태가 남아 있음 | 스크립트가 회차마다 초기화하므로 수동 실행 때만 발생. `down -v` |
| 워밍업 단계에서 이미 포화, 이후 모든 단계 포화 | 워밍업 요청률은 RATES의 첫 값(최대 250)으로 정해지는데, RATES를 한계 근처로 잡아 콜드 JVM이 워밍업에서 무너짐 | `WARMUP_RATE`를 낮게 고정(c10 150, c100 50, c1000 5)하고 `WARMUP_SEC=120` (`run_latency_cooled.sh` 기본값) |
| 회차가 거듭될수록, 또는 측정 도중 느려짐 | 팬 없는 노트북의 열 스로틀링 | 저전력 모드, 회차 사이 휴지, 시작 전 확인(`host_gate.sh`) |
| k6 요약 파일 저장 실패 | k6는 결과 디렉토리를 만들지 못함 | 실행 스크립트가 미리 만든다. k6를 직접 돌릴 때는 `mkdir -p` 먼저 |

### 개선 후 측정 규칙

`BASELINE-pc2.md`의 "개선 결과와 비교할 때의 규칙"과 같다. 핵심만 적는다.

1. 같은 장비·같은 전원 모드, 같은 RATES, 같은 시드 조건, 같은 `WIN_RATIO`로 3회 반복하고, "모든 회차 통과 최대 처리량"으로 비교한다.
2. 코드를 바꾼 뒤에는 `BUILD=1`로 다시 빌드하고, `env.json`에서 `images.bidder.created`가 `appSources.bidder.lastCommit`보다 뒤인지 확인한다. 커밋하지 않은 변경으로 측정하면 `appSources.*.workingTreeDirty`가 true로 남는다.
3. 한 번에 하나만 바꾼다. 트레이싱 샘플링 100%, Kafka 메시지마다 INFO 로그는 기준선의 일부이므로 바꾸려면 별도 측정으로 분리한다.
4. 개선마다 예산 정합성 회귀 검사를 돌린다: `REPEAT=3 RATE=500 BUDGET=5000 perf/scripts/run_oversell.sh <개선이름>-pc2-oversell-r500-b5000`
5. 결과 이름은 `<개선이름>-pc2-c<N>` 형식으로 하고, 요약은 `summarize.sh -o perf/results/<개선이름>.md <접두사>`로 만든다.

## 4. 개선 작업 계획

### 우선순위

| 순서 | 개선 | 근거 (측정) | 기대 효과 | 검증 | 코드 위치 |
|---:|---|---|---|---|---|
| 1 | **캠페인 로컬 캐시** | 요청마다 `SMEMBERS` + `HGETALL` N회, 단일 Lettuce 스레드 포화 | 요청당 `HGETALL` N → 0, 처리 용량이 N에 거의 무관해짐 (메모리 내 필터링 O(N)은 남음) | 처리 용량 전 조건 + 정합성 회귀 | `bidder/.../adapter/out/redis/CampaignAdapter.java` |
| 2 | **요청 타임아웃·부하 차단** | 한계 초과 시 대기열 누적, p50 수 초, 처리량 저하 | 한계를 넘어도 p99 상한 유지, 초과분은 빠르게 204 | **과부하 시나리오 필요** (5장) | `BidController`, `BidService`, Lettuce 요청 대기열 설정 |
| 3 | 매핑을 I/O 스레드에서 분리 (`publishOn`) | 2 vCPU 중 1코어만 사용 | 최다 스레드 사용량 감소, bidder CPU를 200%까지 사용 | 처리 용량 (최다 스레드 코어, 처리 용량) | `CampaignAdapter.loadCampaign` |
| 4 | 만료 리스너 스레드 풀 지정 | 메시지마다 새 스레드 생성. 일반 모드 20분 지속 측정에서 스레드 약 96만 개, 환불이 붙으면 bidder CPU가 크게 증가 | 환불 처리 비용 감소, 스레드 생성 중단 | **지속 부하 고정 요청률 비교** (단계 측정에는 거의 안 보임) + **정합성 회귀 필수** (환불 경로) | `bidder/.../config/RedisKeyExpirationConfig.java` (`container.setTaskExecutor`) |
| 5 | 환불 구조 재설계 (후보) | 지속 측정에서 미환불 예약이 멈추지 않고 증가(만료 감지 지연). 만료 알림은 pub/sub이라 인스턴스 수만큼 중복 처리, 재시작 중 유실, 1시간 뒤 추적 키 만료 알림까지 수신 | 예: 예약을 만료 시각 순 ZSET에 넣고 주기 작업이 묶음 환불. 알림 유실·중복 없음, 예산 묶임 시간 단축 | 지속 부하(미환불 예약 추이) + 정합성 회귀 + 장애 주입(5장) | `RedisExpiredKeyHandler`, `RedisKeyExpirationConfig`, `refund_budget.lua` |

- 4·5를 측정하기 전에 `run_sustained.sh`에 진입 구간(워밍업 요청률에서 목표 요청률까지 60초 동안 서서히 올리고 판정에서 제외)을 추가한다. 지금은 한 번에 올려 첫 1분이 불리하다. 그 뒤 c10 600 req/s 10분 유지를 개선 전후로 각 3회 비교한다(bidder CPU, 미환불 예약 추이, p99).
- 1과 3은 둘 다 Lettuce 스레드 부담을 줄이므로, 효과를 구분하려면 따로 적용해 각각 측정한다. 3을 먼저 단독으로 측정하면 "스레드 분리만으로 얻는 효과"와 "조회 자체를 없애는 효과"를 비교할 수 있다.
- 1의 설계 시 고려할 것: 캐시 갱신 방식(ad_manager 활성화·비활성화 시 Redis pub/sub 무효화 + 주기적 전체 재적재 등), 비활성화된 캠페인이 캐시에 남아 있는 동안의 입찰 허용 범위. 예산 예약은 계속 Redis Lua로 하므로 예산 정합성은 캐시와 무관하다.

### 작은 개선 (측정으로 발견)

- `CampaignAdapter.mapToDomain`이 요청·캠페인마다 `DateTimeFormatter.ofPattern(...)`을 새로 만든다 → 상수로.
- `AuctionTrackingAdapter.storeAuctionTracking`이 `HMSET`과 `EXPIRE`를 두 번 왕복한다 → 한 번으로.
- 추적 정보 TTL 1시간(`TRACKING_TTL`): 입찰 1건당 Redis 메모리 약 0.47 KB, 1,000 req/s가 1시간 지속되면 약 1.7 GB. TTL·저장 항목 재검토.
- Kafka 메시지마다 레코드 전체를 INFO 로그로 남긴다(`adapter/out/messaging/*KafkaAdapter.java`), 트레이싱 샘플링 100%(`bidder/src/main/resources/application.yml`). 캐시 적용 후 다음 병목 후보(저전력 모드 c10 750 req/s에서 HTTP 0.14코어, Kafka 0.10코어, 로그 0.04코어).

### 성능 외 문제 (측정 준비 중 발견)

- **ad_manager: 타겟 필드에 null이 있으면 활성화 실패(500).** `ad_manager/.../redis/CampaignRedisHashMapper.java`의 `Jackson2HashMapper`(flatten)가 null을 처리하지 못한다. 수정안: 매퍼 생성 시 `objectMapper.copy().setSerializationInclusion(JsonInclude.Include.NON_NULL)` + null 타겟 활성화 테스트 추가. README의 API 예시는 타겟을 모두 채워 이 문제를 피한다. 측정용 `seed.sh`도 타겟을 모두 채운다.
- **win 응답이 확정 성공 여부와 무관하게 항상 204** (`WinController`). 확정 실패를 구분할 수 없다.

### 포트폴리오 리뷰에서 나온 기타 후보 (측정 범위 밖)

- 낙찰가(clearing price) 반영: win에서 `${AUCTION_PRICE}`를 받아 `confirm_budget.lua`에서 예약액과의 차액을 환불. 애드테크 지원 시 효과가 크다.
- 만료 알림 유실 대비: `reservation_backup` 키에 TTL이 없고 보정 작업이 없어, 알림을 놓치면 예산이 `budget_reserved`에 묶인다 → 주기적 보정(sweeper).
- log-consumer: `auctionId` unique 제약으로 재처리 시 중복 저장 방지, `ddl-auto: create`(재시작 시 테이블 초기화) 변경.
- bidder: 운영 코드 `main`에서 BlockHound 설치(`BiddingApplication`), `Hooks.enableAutomaticContextPropagation()` 호출 순서, 광고 마크업 URL의 `localhost:8080` 하드코딩(`BidService`).

## 5. 과부하·장애 내성 측정 설계 방향 (미구현)

**과부하 내성**
- 처리 한계의 1.5~2배 부하를 3~5분 유지한 뒤 한계의 50%로 낮춰 회복을 본다.
- 지표: p99, 초과분 거절(204) 비율, 에러율, 힙·GC, OOM 여부, **회복 시간**(부하를 낮춘 뒤 p99가 SLA로 돌아오기까지).
- 구현: `run_latency.sh`는 포화가 감지되면 이후 단계를 건너뛰므로 그대로 쓸 수 없다. `lib.sh`를 재사용하는 `run_overload.sh`와 `ramping-arrival-rate` 기반 k6 스크립트를 새로 만든다.
- 개선 2(타임아웃·부하 차단)의 효과를 보여주는 측정이다. 개선 전 결과를 먼저 남긴다.

**장애 주입 (신뢰성)**
- 예약이 쌓인 상태에서 bidder를 멈췄다가 TTL 이후 재시작 → `verify_budget.sh`로 `budget_reserved`에 묶인 예산(만료 알림 유실)을 확인한다.
- Redis 재시작 시 예산·예약 상태 확인.
- 만료 알림 유실 대비(sweeper) 개선의 전후 비교에 쓴다.

## 6. 측정 방법을 이렇게 정한 이유

다음 세션에서 같은 판단을 다시 하지 않도록 남긴다.

| 결정 | 이유 |
|---|---|
| k6 `constant-arrival-rate`(요청률 고정) | VU 수 고정 방식은 서버가 느려지면 요청을 덜 보내 지연이 실제보다 좋게 나온다(coordinated omission). RTB 거래소는 서버 상태와 무관하게 요청을 보낸다 |
| bidder 2 vCPU / 1 GiB, 힙 512 MiB 고정, G1 명시 | 측정마다 조건을 같게 하기 위함. 메모리 2 GB 미만 컨테이너에서는 JVM이 Serial GC를 자동 선택해 p99가 왜곡되므로 G1을 명시 |
| 워밍업 요청률을 낮게 고정, 120초 | 콜드 JVM에 한계 근처 부하를 주면 JIT 전 쌓인 대기열이 해소되지 않는다(원본 zip의 `archive-pc2-attempt1/`에서 확인). 120초로 늘리자 단계 전환 순간의 p99 급증도 사라졌다 |
| 처리 용량은 저전력 모드에서 측정 | 팬 없는 노트북은 일반 모드에서 측정 도중 열 스로틀링이 생겨, 발열이 시작되는 시점에 따라 회차 결과가 흔들린다(원본 zip의 `archive-pc2-attempt2/`). 회차 사이 휴지나 시작 전 확인으로는 측정 도중의 발열을 막을 수 없다 |
| 시작 전 확인, 회차 사이 8분 휴지, 측정 후 스택 내림 | 직전 회차의 열과 남은 부하(밀린 환불 등)가 다음 회차에 섞이지 않게 하기 위함 |
| p99에 단계 전환 직후 구간 포함 | 부하 급증을 흡수하는 능력까지 포함한 값으로 해석한다 |
| 기준값 = 모든 회차 통과 최대 처리량 | 회차별 최대값의 중앙값은 경계값(일부 회차만 통과)을 포함할 수 있어 과장된다 |
| 처리 용량 측정 `WIN_RATIO=0` | 낙찰률이 낮은 실제 RTB와 비슷하게, 정상 상태에서 입찰 1건마다 만료 환불 1건이 따라붙는 조건 |
| 처리 용량 시드: 모든 캠페인이 입찰 후보 | 필터링으로 걸러지는 캠페인이 없는 최악 조건. 캐시 적용 후에도 같은 조건으로 비교해야 한다 |
| 지속 부하 측정을 따로 둠 | 60초 단계 측정에서는 30초 뒤 시작되는 만료 환불의 대부분이 단계 뒤로 밀려 환불 비용이 거의 반영되지 않는다 |
| 정합성 부하 시간 < 예약 TTL(30초) | TTL을 넘기면 만료 환불된 예산이 다시 쓰여 "성공 입찰 수 ≤ 용량" 판정이 성립하지 않는다 |
| 정합성 예산 용량 ≈ 전체 요청의 절반 | 용량이 작으면 경합이 처음 0.2초에만 일어난다. 경합 구간을 10초 이상으로 늘려 검증을 강하게 한다 |
| 트레이싱 100%, 메시지별 INFO 로그를 기준선에 포함 | 개선과 함께 끄면 어느 쪽 효과인지 구분할 수 없다 |

## 7. 남은 정리 거리

- 스레드별 CPU 측정은 측정 시작과 끝에 모두 살아 있는 스레드만 집계해, 수명이 짧은 스레드(만료 리스너 스레드)의 CPU가 빠진다. JVM 프로세스 전체 CPU(`/proc/1/stat`)를 함께 기록해 차이를 별도 항목으로 남기도록 `run_latency.sh`의 `thread_snapshot`을 보완한다(측정값에는 영향 없음).
- `run_sustained.sh`에 진입 구간 추가 (4장 참고). `summarize.sh`는 지속 측정 결과(`sustained-summary.json`)를 요약하지 못하므로 필요하면 함께 확장한다.
- 지속 측정 결과(`perf/results/sustained-pc2-c10-r*`, 저전력 모드, 진입 구간 없음, 요청률당 1회)는 참고 자료다. c100 지속 측정은 하지 않았다.
- 개선 작업 결과가 나오면 `performance-verification.md`와 README의 성능 검증 절을 함께 갱신한다.
