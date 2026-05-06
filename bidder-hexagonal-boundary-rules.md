# Bidder Hexagonal Boundary Rules

작성일: 2026-04-14

이 문서는 `bidder` 모듈의 헥사고날 아키텍처 경계를 짧게 고정하기 위한 기준 문서다.
작업 중 판단이 애매하면 이 문서를 우선 기준으로 삼는다.

## 1. 목적

- inbound adapter, application service, domain, outbound adapter의 책임을 섞지 않는다.
- "지금 데이터가 여기 있으니 여기서 처리" 같은 편의 구현으로 경계를 무너뜨리지 않는다.
- 변경 중 빠르게 판단할 수 있는 최소 규칙만 유지한다.

## 2. 패키지 역할

### `adapter.in`

- HTTP, query param, request/response DTO, status code, redirect 같은 입출력 표현만 담당한다.
- `domain.port.in`만 호출한다.
- `domain.port.out` 또는 Redis/Kafka/DB adapter를 직접 호출하지 않는다.

예:
- `BidController`
- `WinController`
- `ImpressionController`
- `ClickController`

### `application.service`

- 유스케이스 오케스트레이션을 담당한다.
- 여러 port를 조합해 흐름을 완성한다.
- tracking 조회, 예산 예약/확정, metadata 조회, 비동기 발행 같은 흐름 제어는 여기서 한다.

예:
- `BidService`
- `WinService`
- `ImpressionService`
- `ClickService`

### `domain`

- 도메인 모델과 inbound/outbound port만 가진다.
- 프레임워크나 Redis/Kafka 세부사항을 몰라야 한다.

예:
- `Bid`, `Win`, `Impression`, `Click`, `AuctionTracking`
- `BidUseCase`, `WinUseCase`, `LoadAuctionTrackingPort`

### `adapter.out`

- `domain.port.out`의 구현체다.
- Redis/Kafka/DB와 직접 연결한다.
- 외부 저장소 포맷을 도메인에 맞게 변환한다.

예:
- `BudgetReserveAdapter`
- `AuctionTrackingAdapter`
- `CampaignClickUrlAdapter`

## 3. 의존 방향

허용:
- `adapter.in -> domain.port.in`
- `application.service -> domain.port.in / domain.port.out / domain.model`
- `adapter.out -> domain.port.out / domain.model`

금지:
- `adapter.in -> domain.port.out`
- `adapter.in -> adapter.out`
- `domain -> application.service`
- `domain -> adapter.*`

## 4. 현재 bidder 기준 규칙

### bid

- controller는 request DTO를 command로 바꾸고 use case만 호출한다.
- service가 campaign 조회, reserve, tracking 저장, bid 응답 생성, 비동기 발행을 오케스트레이션한다.

### win / imp / click

- controller는 `aid` 같은 입력만 받아 use case에 넘긴다.
- service가 `auction_tracking`을 조회해 `requestId`, `campaignId`, `creativeId`를 복원한다.
- click의 경우 service가 metadata projection에서 `clickUrl`을 조회한다.

## 5. 냄새 신호

아래가 보이면 경계가 무너졌을 가능성이 크다.

- controller가 `LoadAuctionTrackingPort`, `LoadClickUrlPort`, Redis adapter를 직접 주입받는다.
- controller가 `campaignId`, `creativeId`, `requestId`를 복원하거나 검증한다.
- service가 HTTP redirect나 status code를 직접 만진다.
- outbound adapter가 request DTO를 직접 다룬다.
- domain model이 Redis hash field 이름이나 Kafka record를 안다.

## 6. 리뷰 체크리스트

- controller가 inbound port 외의 의존성을 갖고 있지 않은가
- application service가 유스케이스 흐름을 끝까지 오케스트레이션하고 있는가
- outbound adapter가 외부 저장소 접근을 캡슐화하고 있는가
- domain이 프레임워크나 저장소 세부사항을 모르고 있는가
- "편해서 여기서 했다"는 이유로 경계 위반이 들어오지 않았는가

## 7. 판단 원칙

애매하면 이렇게 판단한다.

1. HTTP/입력 표현 문제인가  
그러면 `adapter.in`

2. 여러 의존성을 조합하는 유스케이스 흐름인가  
그러면 `application.service`

3. 외부 저장소/메시징 접근인가  
그러면 `adapter.out`

4. 순수 상태/규칙/모델인가  
그러면 `domain`

## 8. 권장 후속 조치

- 중요한 경계 규칙은 `AGENTS.md`에도 넣어 작업 시작 전에 다시 보이게 한다.
- 가능하면 나중에 ArchUnit으로 `adapter.in -> port.out 금지`를 자동 검증한다.

