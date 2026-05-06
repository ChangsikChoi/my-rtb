# Reactive Budget Bidding Platform

Redis Lua 기반 예산 정합성과 `auctionId` 중심 추적 구조를 갖춘 실시간 광고 입찰 포트폴리오 프로젝트입니다.

## 프로젝트 한 줄 요약

- `ad_manager`: 캠페인 원본 관리 + Redis projection 반영
- `bidder`: 실시간 입찰, 예산 예약/확정/환불, 이벤트 발행
- `log-consumer`: Kafka 이벤트를 PostgreSQL에 적재

## 왜 이 프로젝트를 만들었는가

광고 입찰 시스템에서 가장 어려운 지점은 아래 3가지라고 보고 설계했습니다.

1. 상태 정합성: 예산을 “언제, 어떤 키로, 어떻게 원자적으로” 변경할 것인가
2. 추적 가능성: bid -> win -> impression -> click 흐름을 어떤 식별자로 안정적으로 조인할 것인가
3. 경계 관리: 빠르게 동작하는 코드와 유지 가능한 구조 사이의 균형을 어떻게 맞출 것인가

## 핵심 설계 포인트

1. `requestId`와 `auctionId` 분리
- 외부 상관관계 ID(`requestId`)와 내부 상태 변경 키(`auctionId`)를 분리해 예산 mutation 안정성을 높였습니다.

2. Redis Lua 기반 예산 처리
- reservation / confirm / refund를 스크립트 기반으로 처리해 예산 상태 변경의 원자성을 확보했습니다.

3. `auction_tracking` 최소화
- tracking에는 최소 필드만 저장하고, 무거운 메타데이터(`clickUrl`)는 projection 조회로 분리했습니다.

4. 헥사고날 경계 유지
- `adapter.in -> application.service -> adapter.out` 흐름을 유지하고, 서비스에서 오케스트레이션하도록 구성했습니다.

## 시스템 흐름

1. 캠페인 생성: `ad_manager`가 Postgres에 저장(`inactive`)
2. 캠페인 활성화: Redis projection 반영 후 bidder 대상 노출
3. 비딩 요청: `bidder`가 후보 평가 후 Lua reservation 성공 시 bid 응답
4. 후속 이벤트: win/imp/click는 `aid(auctionId)`로 처리하고 tracking으로 복원
5. 로그 적재: `log-consumer`가 Avro 이벤트를 Kafka에서 소비해 Postgres에 저장

## 기술 스택

- Java 17
- Spring Boot
- Spring WebFlux (`bidder`)
- JPA (`ad_manager`, `log-consumer`)
- Redis + Lua
- Kafka + Avro + Schema Registry
- PostgreSQL
- Testcontainers
- k6 (성능 기준선)

## 빠른 실행

```bash
docker compose up -d --build
```

- `bidder`: `http://localhost:8080`
- `ad_manager`: `http://localhost:8088`
- `schema-registry`: `http://localhost:8081`

## 문서 맵

- 설계 의사결정과 작업 타임라인: [portfolio-decision-timeline.md](./portfolio-decision-timeline.md)
- 통합 리뷰(현재 코드 기준): [review-integrated-2026-04-20.md](./review-integrated-2026-04-20.md)
- 통합 작업 히스토리: [work-history-integrated-2026-04-20.md](./work-history-integrated-2026-04-20.md)
- 통합 포트폴리오 평가: [portfolio-evaluation-integrated-2026-04-20.md](./portfolio-evaluation-integrated-2026-04-20.md)
- bidder 경계 규칙: [bidder-hexagonal-boundary-rules.md](./bidder-hexagonal-boundary-rules.md)

## 현재 상태(요약)

- 구조적으로는 식별자 분리, 예산 정합성, 추적 흐름이 정리되어 있습니다.
- 남은 과제는 주로 운영 마감도 개선입니다.
- `log-consumer`의 보존/중복방지/관측성 보강
- 일부 테스트 실행 안정성 정리
