package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.BudgetReservePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BudgetReserveAdapter implements BudgetReservePort {

  private static final String BUDGET_RESERVATION_SECONDES = "30";
  private final ReactiveStringRedisTemplate redisTemplate;
  private final DefaultRedisScript<Boolean> reserveBudgetLuaScript;

  @Override
  public Mono<Boolean> reserveBudget(String campaignId, String auctionId, Long reserveAmountMicro) {
    String totalKey = RedisKeys.campaignTotalBudgetKey(campaignId);
    String reservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);
    String reservationKey = RedisKeys.reservationKey(auctionId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(auctionId);

    return redisTemplate.execute(
            // 레디스 스크립트
            reserveBudgetLuaScript,
            // keys
            List.of(totalKey, reservedKey, reservationKey, reservationBackupKey),
            // args
            String.valueOf(reserveAmountMicro),
            campaignId,
            String.valueOf(System.currentTimeMillis()),
            BUDGET_RESERVATION_SECONDES)
        .single()
        .defaultIfEmpty(false);
  }

}
