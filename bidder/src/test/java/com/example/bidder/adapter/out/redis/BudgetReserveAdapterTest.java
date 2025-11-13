package com.example.bidder.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest
class BudgetReserveAdapterTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Autowired
  private ReactiveStringRedisTemplate redisTemplate;
  @Autowired
  private BudgetReserveAdapter budgetReserveAdapter;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
  }

  @Test
  @DisplayName("예산이 충분하면 Lua 스크립트가 예산을 예약하고 true를 반환한다")
  void reserveBudget_budgetIsSufficient_returnTrue() {
    String requestId = "r1";
    String campaignId = "c1";
    // 저장 결과 조회용 키 생성
    String totalKey = RedisKeys.campaignTotalBudgetKey(campaignId);
    String reservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);
    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);

    // 초기 예산 설정
    redisTemplate.opsForValue().set(totalKey, "10000").block();
    redisTemplate.opsForValue().set(reservedKey, "0").block();

    // 예산 예약 실행
    Mono<Boolean> resultMono = budgetReserveAdapter.reserveBudget(campaignId, requestId, 150_000L);
    // 결과 검증용 데이터 조회 mono 생성
    Mono<String> totalBudget = redisTemplate.opsForValue().get(totalKey);
    Mono<String> reservedBudget = redisTemplate.opsForValue().get(reservedKey);
    Mono<Map<String, String>> reservation = redisTemplate.opsForHash()
        .entries(reservationKey)
        .collectMap(
            entry -> entry.getKey().toString(),
            entry -> entry.getValue().toString()
        );
    Mono<Map<String, String>> reservationBackup = redisTemplate.opsForHash()
        .entries(reservationBackupKey)
        .collectMap(
            entry -> entry.getKey().toString(),
            entry -> entry.getValue().toString()
        );

    // 결과 검증
    StepVerifier.create(resultMono)
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(totalBudget)
        .expectNext("9850")
        .verifyComplete();

    StepVerifier.create(reservedBudget)
        .expectNext("150")
        .verifyComplete();

    StepVerifier.create(reservation)
        .assertNext(
            row -> {
              assertThat(row.get("campaignId")).isEqualTo(campaignId);
              assertThat(row.get("amount")).isEqualTo("150");
            }
        )
        .verifyComplete();

    StepVerifier.create(reservationBackup)
        .assertNext(
            row -> {
              assertThat(row.get("campaignId")).isEqualTo(campaignId);
              assertThat(row.get("amount")).isEqualTo("150");
            }
        )
        .verifyComplete();
  }

  @Test
  @DisplayName("예산이 충분하지 않으면 예산 예약을 하지않고 false를 반환한다")
  void reserveBudget_budgetIsNotSufficient_returnFalse() {
    String requestId = "r1";
    String campaignId = "c1";
    // 저장 결과 조회용 키 생성
    String totalKey = RedisKeys.campaignTotalBudgetKey(campaignId);
    String reservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);
    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);

    // 초기 예산 설정
    redisTemplate.opsForValue().set(totalKey, "100").block();
    redisTemplate.opsForValue().set(reservedKey, "0").block();

    // 예산 예약 실행
    Mono<Boolean> resultMono = budgetReserveAdapter.reserveBudget(campaignId, requestId, 200_000L);
    // 결과 검증용 데이터 조회 mono 생성
    Mono<String> totalBudget = redisTemplate.opsForValue().get(totalKey);
    Mono<String> reservedBudget = redisTemplate.opsForValue().get(reservedKey);
    Mono<Map<String, String>> reservation = redisTemplate.opsForHash()
        .entries(reservationKey)
        .collectMap(
            entry -> entry.getKey().toString(),
            entry -> entry.getValue().toString()
        );
    Mono<Map<String, String>> reservationBackup = redisTemplate.opsForHash()
        .entries(reservationBackupKey)
        .collectMap(
            entry -> entry.getKey().toString(),
            entry -> entry.getValue().toString()
        );

    // 결과 검증
    StepVerifier.create(resultMono)
        .expectNext(false)
        .verifyComplete();

    StepVerifier.create(totalBudget)
        .expectNext("100")
        .verifyComplete();

    StepVerifier.create(reservedBudget)
        .expectNext("0")
        .verifyComplete();

    StepVerifier.create(reservation)
        .assertNext(
            row -> assertThat(row).isEmpty()
        )
        .verifyComplete();

    StepVerifier.create(reservationBackup)
        .assertNext(
            row -> assertThat(row).isEmpty()
        )
        .verifyComplete();

  }
}