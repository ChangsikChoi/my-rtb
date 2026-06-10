# log-consumer Kafka Listener Test Plan

작성일: 2026-05-11

## 1. 목적

`log-consumer`의 현재 테스트는 아래 범위를 검증한다.

- Avro message -> JPA entity 매핑 단위 테스트
- PostgreSQL Testcontainers 기반 repository 저장/조회 테스트
- Spring context load 테스트

아직 검증하지 못한 영역은 Kafka 운영 경로다.

- `@KafkaListener`가 실제 topic에서 메시지를 읽는지
- Avro deserializer와 Schema Registry 설정이 실제로 동작하는지
- consumer 처리 실패 시 `@RetryableTopic` retry topic으로 이동하는지
- retry 소진 후 DLT로 이동하고 `@DltHandler`가 호출되는지
- 장애 분석에 필요한 topic/key/partition/offset/error metadata를 관측할 수 있는지

이 문서는 위 Kafka listener/retry/DLT 경로의 테스트 계획을 고정한다.

## 2. 현재 구현 기준

대상 consumer:

- `BiddingLogConsumer`
- `WinLogConsumer`
- `ImpressionLogConsumer`
- `ClickLogConsumer`

현재 공통 특성:

- `@KafkaListener(topics = "...", groupId = "...")`
- `@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2))`
- `@DltHandler`는 DLT logger에 warning 로그를 남김
- 메시지 key는 producer 쪽 기준 `auctionId`
- value는 Avro generated class
- DB sink는 JPA repository

## 3. 테스트 레벨 구분

### 3.1 유지할 테스트

이미 추가한 테스트는 유지한다.

- consumer 단위 테스트: Kafka 없이 message mapping과 repository save 호출 검증
- repository 테스트: PostgreSQL 저장/조회와 `createdAt` auditing 검증

이 테스트들은 빠른 회귀 검증용이다.

### 3.2 추가할 테스트

Kafka listener 운영 경로는 별도 통합 테스트로 둔다.

- Kafka broker 필요
- Schema Registry 필요
- PostgreSQL 필요
- listener auto-start 필요
- retry/DLT topic 생성과 consume 검증 필요

기존 `LogConsumerIntegrationTestSupport`는 Kafka listener 자동 시작을 꺼두므로, Kafka listener 통합 테스트용 support를 별도로 만든다.

예상 support:

- `LogConsumerKafkaIntegrationTestSupport`
- PostgreSQL Testcontainers
- Kafka Testcontainers
- Schema Registry Testcontainers
- `spring.kafka.listener.auto-startup=true`
- `spring.kafka.bootstrap-servers` 동적 주입
- `spring.kafka.schema-registry.url` 동적 주입

## 4. 테스트 시나리오

## 4.1 Listener Happy Path

목표:

- 실제 Kafka topic에 Avro 메시지를 publish했을 때 listener가 consume하고 PostgreSQL에 저장하는지 검증한다.

대상:

- `bidding-log`
- `win-log`
- `impression-log`
- `click-log`

검증:

- topic에 Avro message publish
- listener consume 대기
- repository에서 row 조회
- `auctionId`, `requestId`, `campaignId`, `creativeId`, `receivedAt` 검증
- `BiddingLog`는 `priceMicro` 검증
- `createdAt` not null 검증

주의:

- 비동기 consume이므로 Awaitility 또는 polling helper를 사용한다.
- 테스트 key는 `auctionId`로 고정한다.
- 테스트마다 고유 `auctionId`를 사용해 row 충돌을 피한다.

## 4.2 Avro Deserialization Failure

목표:

- 깨진 payload 또는 schema 불일치 메시지에서 `ErrorHandlingDeserializer` 경로가 동작하는지 확인한다.

검증 후보:

- invalid Avro payload publish
- listener method가 정상 message로 호출되지 않음
- error handler 또는 DLT 경로로 이동하는지 확인

주의:

- 현재 `KafkaConsumerConfig`에는 deserialization error 전용 `DefaultErrorHandler`/DLT publishing recoverer가 명시되어 있지 않다.
- 이 테스트는 현재 구현으로 바로 통과하지 않을 수 있다.
- 먼저 기대 동작을 정해야 한다.
  - 역직렬화 실패도 DLT로 보낼 것인가
  - 아니면 error log 후 skip할 것인가

## 4.3 Repository Save Failure Retry

목표:

- listener가 정상 Avro 메시지를 읽었지만 DB 저장에서 예외가 발생하면 retry topic으로 이동하는지 검증한다.

테스트 구성:

- 테스트 전용 `@TestConfiguration`에서 특정 repository bean을 실패하도록 대체한다.
- 예: `save()` 호출 시 `DataAccessResourceFailureException` 발생
- retry topic consumer 또는 Kafka admin으로 topic record를 관측한다.

검증:

- 원본 topic consume 시도
- retry topic으로 record 이동
- attempts 설정에 따라 retry가 진행됨
- 최종적으로 DLT topic에 record 도착

주의:

- `@RetryableTopic`은 retry topic 이름을 자동 생성한다.
- topic 이름 규칙을 테스트에 직접 박기보다 Spring Kafka의 retry topic naming strategy를 확인하고 문서화한다.
- backoff 시간을 그대로 기다리면 테스트가 느려진다. 통합 테스트 profile에서 backoff를 짧게 override할 수 있도록 설정 분리가 필요하다.

## 4.4 DLT Handler Invocation

목표:

- retry 소진 후 `@DltHandler`가 호출되는지 검증한다.

현재 한계:

- `@DltHandler`는 logger에만 기록한다.
- 테스트에서 logger 호출을 검증하는 방식은 취약하다.

권장 선행 작업:

- DLT 처리를 별도 component로 분리한다.
  - 예: `DltLogHandler`
  - consumer의 `@DltHandler`는 해당 component에 위임
- 테스트에서는 `@SpyBean` 또는 mock bean으로 handler 호출을 검증한다.

검증:

- 실패 메시지 publish
- retry 소진 대기
- DLT handler가 message와 topic metadata를 받았는지 확인

## 4.5 Backoff/Attempt Policy

목표:

- `attempts = 3`, `delay = 1000`, `multiplier = 2` 정책이 의도와 맞는지 검증한다.

권장 방식:

- 실제 시간 지연을 정밀 검증하지 않는다.
- flaky test를 피하기 위해 아래 중 하나로 제한한다.
  - annotation metadata 검증
  - retry topic 이동 횟수 검증
  - 테스트 profile에서 backoff를 짧게 override한 뒤 retry sequence만 검증

향후 개선:

- backoff/attempt 값을 property로 외부화한다.
- 테스트 profile에서 짧은 값으로 설정한다.

## 4.6 DLT Observability Metadata

목표:

- DLT 처리 시 운영 분석에 필요한 metadata를 남기는지 검증한다.

필요 metadata:

- original topic
- received topic
- partition
- offset
- key
- exception class
- exception message
- retry attempt
- `auctionId`
- `requestId`

현재 구현 한계:

- `@DltHandler`는 message와 received topic만 로그에 남긴다.
- partition/offset/key/error metadata가 없다.

권장 선행 작업:

- DLT handler signature에 header 인자를 추가한다.
- 또는 DLT 처리 component에 `ConsumerRecord`/headers 기반 metadata를 전달한다.

테스트:

- 실패 메시지를 DLT까지 이동시킨다.
- handler에 전달된 metadata 또는 저장된 failure record를 검증한다.

## 5. 구현 순서

### Step 1. Kafka 통합 테스트 support 추가

- `LogConsumerKafkaIntegrationTestSupport` 추가
- PostgreSQL + Kafka + Schema Registry Testcontainers 구성
- listener auto-start 활성화
- 테스트 전용 topic suffix 또는 고유 group id 전략 결정

완료 기준:

- 빈 Kafka listener context가 정상 기동한다.
- 외부 Kafka/Schema Registry 없이 테스트가 실행된다.

### Step 2. Listener Happy Path 테스트

- 4개 topic에 대해 정상 Avro publish -> DB 저장 검증
- 먼저 `BiddingLogConsumer` 1개로 패턴 확정
- 이후 win/impression/click으로 확장

완료 기준:

- Kafka round-trip 후 PostgreSQL row 저장이 검증된다.

### Step 3. Retry/DLT 테스트 가능 구조로 리팩터링

- DLT handler를 별도 component로 분리
- retry/backoff 설정을 property로 외부화
- 테스트 profile에서 backoff를 짧게 설정

완료 기준:

- 실패 경로를 테스트에서 안정적으로 유도할 수 있다.

### Step 4. Repository Save Failure -> Retry -> DLT 테스트

- repository save 실패 유도
- retry topic 이동 확인
- DLT handler 호출 확인

완료 기준:

- DB 장애 시 retry와 DLT 경로가 자동 테스트로 검증된다.

### Step 5. Deserialization Failure 정책 결정 및 테스트

결정한 정책:

- 역직렬화 실패는 별도 DLT topic으로 보내지 않는다.
- `ErrorHandlingDeserializer`가 전달한 실패 record를 error handler에서 로그로 남긴다.
- 실패 record는 retry하지 않고 skip한다.
- invalid record 이후에도 consumer가 다음 정상 메시지를 계속 처리할 수 있어야 한다.
- 추후 raw payload 보존 또는 재처리 요구가 생기면 deserialization 전용 DLT topic을 별도로 추가한다.

작업 순서:

1. `KafkaDeserializationFailureLogger` 추가
   - topic, partition, offset, key, exception class/message를 로그로 남긴다.
   - error handler에서 호출하기 쉽도록 `ConsumerRecord`와 예외를 인자로 받는다.
2. `KafkaConsumerConfig`에 `DefaultErrorHandler` 연결
   - deserialization failure record를 retry하지 않고 로그 후 skip한다.
   - 기존 repository save failure retry/DLT 경로와 충돌하지 않게 한다.
3. 테스트용 raw byte publish helper 추가
   - Avro serializer를 거치지 않고 invalid bytes를 Kafka topic에 publish한다.
4. `BiddingLogKafkaDeserializationFailureTest` 추가
   - invalid payload에서 repository/listener processing이 호출되지 않는지 확인한다.
   - deserialization failure logger가 호출되는지 확인한다.
   - invalid payload 이후 valid payload가 정상 저장되는지 확인한다.
5. 기존 listener/retry/DLT 테스트 재실행
   - happy path와 repository save failure retry/DLT 경로가 유지되는지 확인한다.

완료 기준:

- 깨진 메시지가 consumer를 죽이거나 partition 처리를 막지 않는다.
- 역직렬화 실패 정보가 로그로 남는다.
- invalid record 이후의 정상 메시지가 저장된다.

### Step 6. DLT Observability 테스트

- DLT metadata 수집 구조 개선
- metadata 검증 테스트 추가

완료 기준:

- DLT 발생 시 장애 분석에 필요한 최소 metadata가 테스트로 보장된다.

## 6. 우선순위

1. Listener happy path 통합 테스트
2. DLT handler 분리
3. repository save failure retry/DLT 테스트
4. DLT metadata 관측성 테스트
5. deserialization failure 정책 결정 및 테스트

## 7. 이번 범위에서 제외

아래 항목은 Kafka 운영 경로 테스트 이후 별도 작업으로 둔다.

- idempotency unique constraint 구현
- Kafka metadata DB 저장
- DLT message DB 저장 테이블 설계
- 운영 profile의 `ddl-auto` 제거와 migration 도입
- consumer concurrency/partition rebalance 테스트
