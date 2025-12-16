package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.BudgetConfirmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BudgetConfirmAdapter implements BudgetConfirmPort {

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Mono<Boolean> confirmBudget(String requestId, String campaignId) {
    String confirmScript = LuaScripts.CAMPAIGN_BUDGET_CONFIRM.loadScript();

    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);
    String budgetReservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);

    return redisTemplate.execute(
            RedisScript.of(confirmScript, Boolean.class),
            List.of(reservationKey, reservationBackupKey, budgetReservedKey))
        .single()
        .defaultIfEmpty(false);
  }

}
