package com.example.bidder.adapter.out.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisExpiredKeyHandler {

    private final StringRedisTemplate redisTemplate;

    public void handleExpiredKey(String expiredKey) {
        if (!expiredKey.startsWith(RedisKeys.RESERVATION_PREFIX)) {
            return;
        }
        String requestId = expiredKey.split(RedisKeys.RESERVATION_PREFIX)[1];
        String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);

        String refundScript = LuaScripts.CAMPAIGN_BUDGET_REFUND.loadScript();
        Boolean success = redisTemplate.execute(
                RedisScript.of(refundScript, Boolean.class),
                List.of(reservationBackupKey));
        if (success) {
          //TODO: 카프카 토픽 생성 후 이벤트 발행 로직 추가
          log.info("Budget Refund Lua Script Executed Successfully");
        } else {
          log.info("Budget Refund Lua Script Execution Failed");
        }
        log.info("Budget Refund Lua is Done");
    }
}
