# Reactive Budget 통합 리뷰 (현재 코드 기준)

작성일: 2026-04-20

## 1. 범위와 기준

이 문서는 기존 리뷰 문서들을 현재 코드 상태로 재검증해 하나로 통합한 결과다.

- 주요 입력 문서
  - `ad_manager/review.md`
  - `log-consumer/review-2026-04-13.md`
  - `portfolio-evaluation-2026-04-17.md`
  - `research.md`
- 검증 방식
  - 소스 코드 직접 점검
  - 모듈별 `./gradlew test --rerun-tasks` 실행
- 제외 기준
  - 현재 코드에서 해결된 항목
  - 현재 코드에서 재현/확인 근거를 찾지 못한 항목

## 2. 현재 검증 결과

- `bidder`
  - 테스트 컴파일 실패
  - 원인: `WinResultKafkaAdapterTest`에서 `WinBuilder.id(...)` 호출(현재 모델과 불일치)
- `ad_manager`
  - 테스트 40개 중 3개 실패
  - 실패 원인: Testcontainers Docker 환경 미가동(`IllegalStateException at DockerClientProviderStrategy`)
- `log-consumer`
  - `BUILD SUCCESSFUL`
  - 단, 테스트 자산은 `contextLoads` 1건 중심

## 3. 모듈별 통합 리뷰

### 3.1 bidder

#### 확인된 강점

- 식별자 분리(`requestId`/`auctionId`)가 코드에 반영됨
  - 예약/확정/환불 키, Kafka key, 후속 엔드포인트가 `auctionId` 기반
- 후속 이벤트 흐름에서 `aid` 중심으로 tracking 복원 구조 적용
- Kafka callback null-safe 처리 반영
- click open redirect 취약점(외부 `url` 직접 입력) 제거

#### 현재 남아 있는 이슈

1. 테스트 컴파일 불일치
- 영향: 회귀 검증 파이프라인이 즉시 깨짐
- 근거: `bidder/src/test/java/com/example/bidder/adapter/out/messaging/WinResultKafkaAdapterTest.java`

2. 후속 엔드포인트 오류 신호 소실
- `WinController`, `ImpressionController`, `ClickController`가 실패를 `204`로 흡수
- 영향: 운영 중 장애/정상 no-content 구분이 어려움

3. bid 응답 URL 하드코딩
- `BidService`에 `http://localhost:8080` 직접 포함
- 영향: 환경별 배포/테스트 시 설정 일관성 저하

4. 성능 핫패스 구조 한계
- 요청마다 활성 캠페인 전체 로드 후 메모리 필터/정렬
- 영향: 캠페인 수 증가 시 Redis fan-out/메모리 비용 증가 가능

5. confirm 검증 강도
- `confirm_budget.lua`는 예약 amount 중심 처리
- reservation/tracking 간 교차검증 강화 여지 존재

### 3.2 ad_manager

#### 확인된 강점

- 생성과 집행 노출 분리(`active=false` 생성, activate/deactivate 분리)
- DB 트랜잭션 + Redis 동기 호출 + 보상 트랜잭션 구조 정리
- 활성화 Lua 단일 호출로 projection 반영 원자성 개선
- mapper/DTO/예외/테스트 경계 정리가 전반적으로 반영됨

#### 현재 남아 있는 이슈

1. `remainingBudgetMicro` 필드 기술 부채
- hash 필드와 실제 예산 키 의미가 분리되어 있음
- `bidder`와 함께 정리하는 것이 맞는 상태

2. `CampaignRedisService`의 Lua arg 직렬화 결합
- 서비스가 Lua 계약 순서를 직접 조합
- 계약 변화 시 서비스 변경 범위가 커짐

3. 테스트 실행 환경 의존성
- 통합 테스트가 Docker/Testcontainers 의존
- 로컬/CI 환경 준비 없으면 실패

### 3.3 log-consumer

#### 확인된 강점

- 이벤트 모델이 `auctionId`, `requestId`, `receivedAt`, `createdAt` 축으로 확장됨
- 메시지 key는 `auctionId` 중심 구조와 맞춰짐
- 테스트 실행은 통과(`BUILD SUCCESSFUL`)

#### 현재 남아 있는 이슈

1. 데이터 보존 설정 리스크
- `spring.jpa.hibernate.ddl-auto: create`
- 재기동 시 테이블 재생성 위험

2. 중복 저장 방어 부재
- 소비 메시지를 무조건 insert
- unique constraint/idempotent 전략 부재

3. 관측성 미흡
- consumer/DLT가 `System.out.println` 중심
- topic/partition/offset/retry metadata 기반 운영 로그 부재

4. 테스트 깊이 부족
- 현재 테스트는 사실상 컨텍스트 로드 수준
- listener-retry-dlt-save 경로에 대한 자동 회귀 안전망 부족

## 4. 이번 통합에서 제외된 이슈(해결/근거 부족)

- `requestId`를 Kafka key로 사용한다는 지적: 현재는 `auctionId` key 사용으로 해결됨
- Kafka callback null-safe 누락 지적: null-safe fallback 로직 반영됨
- 클릭 URL open redirect 지적: `aid` 기반 내부 복원으로 해결됨
- 성별 `OTHER` 불일치 지적: 현재 모델에서 성별 타겟 로직 자체가 없음
- `ad_manager`의 생성/활성화 미분리, 이벤트/재시도 과구조 지적: 현재 구조에서 정리됨

## 5. 우선순위 제안

### P0 (즉시)

1. `bidder` 테스트 컴파일 실패 수정
2. `log-consumer` `ddl-auto: create` 제거
3. `log-consumer` `System.out.println` -> 구조화 로깅 전환

### P1 (단기)

1. `log-consumer` idempotency(고유키/업서트) 도입
2. `win/imp/click` 에러 신호 정책 정리(204 일괄 흡수 재검토)
3. `BidService` URL 설정 외부화

### P2 (중기)

1. `ad_manager` Redis projection 기술 부채 정리
2. confirm/refund 검증 강화
3. `log-consumer` 통합 테스트 확충(Kafka + DB + retry/DLT)
