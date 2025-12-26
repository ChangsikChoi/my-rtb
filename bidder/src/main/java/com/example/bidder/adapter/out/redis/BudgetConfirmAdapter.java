package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.BudgetConfirmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BudgetConfirmAdapter implements BudgetConfirmPort {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final DefaultRedisScript<Boolean> confirmBudgetLuaScript;

  @Override
  public Mono<Boolean> confirmBudget(String requestId, String campaignId) {
    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);
    String budgetReservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);

    return redisTemplate.execute(
            confirmBudgetLuaScript,
            List.of(reservationKey, reservationBackupKey, budgetReservedKey))
        .single()
        .defaultIfEmpty(false);
  }

}
