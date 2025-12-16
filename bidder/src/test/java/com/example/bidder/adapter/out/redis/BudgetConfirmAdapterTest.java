package com.example.bidder.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest
class BudgetConfirmAdapterTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Autowired
  private ReactiveStringRedisTemplate redisTemplate;
  @Autowired
  private BudgetConfirmAdapter budgetConfirmAdapter;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
  }

  @Test
  @DisplayName("예약 예산 조회 성공 시 선점한 예산을 차감하고 예약 정보를 삭제 후 true를 반환한다")
  void confirmBudget_reservationFound_returnTrue() {
    String requestId = "r1";
    String campaignId = "c1";
    String reserveBudgetMicro = "1000000";

    // 저장 결과 조회용 키 생성
    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);
    String budgetReservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);

    // 예산 예약 데이터 설정
    HashMap<String, Object> reservationData = new HashMap<>();
    reservationData.put("campaignId", campaignId);
    reservationData.put("amount", reserveBudgetMicro);
    reservationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

    redisTemplate.opsForHash().putAll(reservationKey, reservationData).block();
    redisTemplate.opsForHash().putAll(reservationBackupKey, reservationData).block();
    redisTemplate.opsForValue().increment(budgetReservedKey, Long.parseLong(reserveBudgetMicro))
        .block();

    // 예산 확정 실행
    Mono<Boolean> resultMono = budgetConfirmAdapter.confirmBudget(requestId, campaignId);
    // 결과 검증 (선점 예산 0으로 감소, 예약 정보 삭제)
    Mono<String> reservedBudget = redisTemplate.opsForValue().get(budgetReservedKey);
    Flux<Entry<Object, Object>> reservation = redisTemplate.opsForHash()
        .entries(reservationKey);

    Flux<Entry<Object, Object>> reservationBackup = redisTemplate.opsForHash()
        .entries(reservationBackupKey);

    StepVerifier.create(resultMono)
        .expectNext(true)
        .verifyComplete();
    StepVerifier.create(reservedBudget)
        .expectNext("0")
        .verifyComplete();
    StepVerifier.create(reservation)
        .expectNextCount(0)
        .verifyComplete();
    StepVerifier.create(reservationBackup)
        .expectNextCount(0)
        .verifyComplete();
  }

  @Test
  @DisplayName("예약 예산 조회 실패 시 false를 반환한다")
  void confirmBudget_reservationNotFound_returnFalse() {
    String requestId = "r1";
    String campaignId = "c1";
    String reserveBudgetMicro = "1000000";

    // 저장 결과 조회용 키 생성
    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);
    String budgetReservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);

    // 예산 예약 데이터 설정
    HashMap<String, Object> reservationData = new HashMap<>();
    reservationData.put("campaignId", campaignId);
    reservationData.put("timestamp", String.valueOf(System.currentTimeMillis()));

    redisTemplate.opsForHash().putAll(reservationKey, reservationData).block();
    redisTemplate.opsForHash().putAll(reservationBackupKey, reservationData).block();
    redisTemplate.opsForValue().increment(budgetReservedKey, Long.parseLong(reserveBudgetMicro))
        .block();

    // 예산 확정 실행
    Mono<Boolean> resultMono = budgetConfirmAdapter.confirmBudget(requestId, campaignId);
    // 결과 검증 (선점 예산 감소하지 않음, 예약 정보 삭제되지 않음)
    Mono<String> reservedBudget = redisTemplate.opsForValue().get(budgetReservedKey);
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

    StepVerifier.create(resultMono)
        .expectNext(false)
        .verifyComplete();
    StepVerifier.create(reservedBudget)
        .expectNext(reserveBudgetMicro)
        .verifyComplete();
    StepVerifier.create(reservation)
        .assertNext(row -> assertThat(row.get("campaignId")).isEqualTo(campaignId))
        .verifyComplete();
    StepVerifier.create(reservationBackup)
        .assertNext(row -> assertThat(row.get("campaignId")).isEqualTo(campaignId))
        .verifyComplete();
  }
}