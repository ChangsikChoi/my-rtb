# Reactive Budget 통합 작업 히스토리

작성일: 2026-04-20

## 1. 문서 통합 기준

이 문서는 계획/실행/의사결정/학습 내용을 하나로 합친 히스토리 문서다.

- 통합한 문서군
  - `context_history.md`
  - `research.md`
  - `auction-id-implementation-plan.md`
  - `auction-id-separation-and-openrtb-notes.md`
  - `auction-tracking-alternative-analysis.md`
  - `phase7-predecisions.md`
  - `impl_auction_id-branch-summary.md`
  - `ad_manager/work_history_summary.md`
  - `ad_manager/campaign_lifecycle_notes.md`
  - `ad_manager/temp.md`
  - `performance-baseline.md`
- 제외 문서(요청 반영)
  - `exception_architecture.md`
  - `exception_architecture.stash-2026-04-20.md`

## 2. 작업 타임라인 요약

### Phase A. MVP 출발점

- WebFlux + R2DBC 중심 단일 흐름으로 시작
- 목표는 기능 완성 우선, 고성능 구조는 후속 단계로 유보
- 과금/노출/트래킹 기본 흐름을 먼저 구현

### Phase B. 이벤트 아키텍처 확장

- Kafka Connect 경로를 실험했지만 스키마 제약으로 실효성 낮음 확인
- Kafka Consumer 직접 구현 + Avro + Schema Registry로 전환
- `bidder`(producer) / `log-consumer`(sink) 역할 분리 고정

### Phase C. 경계/정합성 중심 재설계

- `bidder` 헥사고날 경계 룰 명시 및 적용
- `requestId`와 내부 mutation key를 분리하기 위해 `auctionId` 도입
- Redis reservation/tracking/key 설계를 `auctionId` 중심으로 재정렬

### Phase D. 후속 이벤트와 시간축 정리

- win/imp/click를 `aid` 기반으로 단순화
- tracking 복원 후 이벤트 생성 구조로 변경
- 이벤트 시간(`receivedAt`)과 sink 저장 시간(`createdAt`) 분리

### Phase E. ad_manager 단순화/정리

- 생성과 집행 노출 분리(`active=false` 기본)
- 이벤트/재시도 과구조 제거
- DB -> Redis 동기 호출 + 실패 시 보상 트랜잭션 구조로 정리
- 활성화 Lua 단일 호출 및 mapper/DTO/예외/테스트 구조 정리

### Phase F. 성능 측정 기초 자산 도입

- `perf/k6/bid_baseline.js`와 baseline 결과 파일 생성
- 최소 기준선(TPS/p95/실패율) 비교 가능한 형태로 확보

## 3. 핵심 의사결정과 트레이드오프

## 3.1 식별자 전략

- 결정
  - 외부 상관관계: `requestId`
  - 내부 상태 변경/조인 키: `auctionId`
- 이유
  - 외부 입력 신뢰를 내부 불변조건으로 사용하지 않기 위해

## 3.2 tracking 저장 전략

- 대안 A: `auction_tracking`에 `clickUrl`까지 저장
- 대안 B(채택): 최소 필드 저장 + `clickUrl`은 metadata projection 조회
- 채택 이유
  - auction 단위 메모리 증가 억제
  - 필요한 상관관계 정보는 유지

## 3.3 ad_manager 정합성 처리 전략

- 대안 A: 이벤트 + 실패 이력 + 자동 재시도
- 대안 B(채택): 동기 Redis + 보상 트랜잭션
- 채택 이유
  - 현재 프로젝트 목적 대비 설명 비용/복잡도 균형

## 3.4 시간 필드 의미 고정

- `receivedAt`: 요청 수신 시각(입력 경계에서 기록)
- `createdAt`: sink DB row 생성 시각(저장 시 자동 생성)
- 효과
  - 퍼널/지연 분석 시 시간 의미 혼선 감소

## 4. 내가 학습한 내용

1. 외부 계약 신뢰와 내부 불변조건은 분리해야 한다.
2. 실시간 시스템 설계에서는 메모리/키 churn 비용도 도메인 의사결정 항목이다.
3. 헥사고날 경계는 작은 편의 구현으로도 쉽게 무너진다.
4. 이벤트 시간 의미를 먼저 고정하면 이후 구현과 문서가 덜 흔들린다.
5. 구조 개선은 “복잡한 기능 추가”보다 “잘못된 결합 제거”가 더 큰 효과를 낸다.

## 5. 해결된 리뷰 항목 정리

기존 리뷰/리서치에서 지적되었으나 현재 해결된 항목을 정리한다.

1. `requestId` 과신 구조
- `auctionId` 도입으로 상태 변경 키 분리 완료

2. 클릭 open redirect 취약점
- 외부 `url` 파라미터 직접 신뢰 제거, `aid` 기반 복원으로 전환

3. Kafka callback null-safe 누락
- 메시징 어댑터에서 실패 시 fallback 메시지 로깅 처리 적용

4. `ad_manager`의 생성/노출 미분리 문제
- 생성(`inactive`)과 활성화/비활성화를 분리해 안전 기본값 확립

5. `ad_manager`의 이벤트/재시도 과구조
- 보상 트랜잭션 기반 단순 구조로 정리

6. DTO/예외/mapper 경계 혼선
- 네이밍/응답/예외/mapper 역할을 모듈 경계에 맞춰 통일

## 6. 현재 남아 있는 숙제

1. `bidder` 테스트 컴파일 불일치 수정
2. `log-consumer` 데이터 보존/중복방지/관측성 보강
3. `bidder` 후속 엔드포인트 에러 시그널 정책 정리
4. `BidService` URL 하드코딩 제거
5. `ad_manager` projection 기술 부채(`remainingBudgetMicro`, Lua arg 직렬화 결합) 정리

## 7. 다음 작업 제안

### Sprint 1

1. 테스트/설정 안정화
2. `log-consumer` 운영 안전장치(`ddl-auto`, 로깅, idempotency) 반영

### Sprint 2

1. bidder 운영 신호 개선(에러 응답/메트릭/로그)
2. ad_manager projection 계약 정리

### Sprint 3

1. 성능 개선 실험(핫패스 최적화)과 기준선 재측정
2. 포트폴리오 문서와 README를 지표 기반으로 동기화
