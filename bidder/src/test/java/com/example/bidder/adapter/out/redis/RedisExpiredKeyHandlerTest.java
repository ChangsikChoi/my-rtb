package com.example.bidder.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class RedisExpiredKeyHandlerTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @MockitoSpyBean
  StringRedisTemplate stringRedisTemplate;
  @Autowired
  RedisExpiredKeyHandler redisExpiredKeyHandler;

  @BeforeEach
  void setUp() {
    stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
  }

  @Test
  @DisplayName("만료이벤트가 발생한 키의 prefix가 reservation이 아니면 아무 동작도 하지 않는다.")
  void notReservationKey_doNothing() {
    String notExpiredKey = "other_prefix:123";
    redisExpiredKeyHandler.handleExpiredKey(notExpiredKey);
    // 레디스 호출하지 않음
    verify(stringRedisTemplate, times(0)).execute(any(), any(), any());
  }

  @Test
  @DisplayName("예약 만료 이벤트 발생 시 예산 환급 및 백업데이터 삭제가 정상 처리된다.")
  void reservationExpired_refundBudgetAndDeleteBackup() {
    String requestId = "req_1";
    String expiredKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);

    String campaignId = "camp_1";
    String budgetTotalKey = RedisKeys.campaignTotalBudgetKey(campaignId);
    String budgetReservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);

    // 예약 내용 조회용 백업데이터 저장
    stringRedisTemplate.opsForHash().put(reservationBackupKey, "campaignId", campaignId);
    stringRedisTemplate.opsForHash().put(reservationBackupKey, "amount", "100");

    // 선점 예산 설정
    stringRedisTemplate.opsForValue().set(budgetReservedKey, "500");
    // 총 예산 설정
    stringRedisTemplate.opsForValue().set(budgetTotalKey, "1000");

    // 만료 이벤트 핸들러 실행
    redisExpiredKeyHandler.handleExpiredKey(expiredKey);

    // 예산 환급 및 백업데이터 삭제 검증
    String budgetTotalResult = stringRedisTemplate.opsForValue().get(budgetTotalKey);
    String budgetReservedResult = stringRedisTemplate.opsForValue().get(budgetReservedKey);
    Map<Object, Object> reservationBackup = stringRedisTemplate.opsForHash().entries(reservationBackupKey);

    assertThat(reservationBackup).isEmpty();
    assertThat(budgetTotalResult).isEqualTo("1100");
    assertThat(budgetReservedResult).isEqualTo("400");
  }
}