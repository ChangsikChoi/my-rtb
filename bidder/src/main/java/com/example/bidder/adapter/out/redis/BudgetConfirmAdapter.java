package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.BudgetConfirmPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BudgetConfirmAdapter implements BudgetConfirmPort {

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Mono<Boolean> confirmBudget(String requestId) {
    String confirmScript = LuaScripts.CAMPAIGN_BUDGET_CONFIRM.loadScript();
    String reservationKey = RedisKeys.reservationKey(requestId);
    String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);

    return redisTemplate.execute(RedisScript.of(confirmScript, Boolean.class),
            List.of(reservationKey, reservationBackupKey))
        .single()
        .defaultIfEmpty(false);
  }

}
