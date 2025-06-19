package com.example.mybidder.bidding.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetWithLuaScriptService {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> reserveBudget(String campaignId, String requestId, long reserveAmountMicro, Duration ttl) {
        String reserveScript = LuaScripts.CAMPAIGN_BUDGET_RESERVE.loadScript();

        String totalKey = RedisKeys.campaignTotalBudgetKey(campaignId);
        String reservedKey = RedisKeys.campaignReservedBudgetKey(campaignId);
        String reservationKey = RedisKeys.reservationKey(requestId);
        String reservationBackupKey = RedisKeys.reservationBackupKey(requestId);

        return redisTemplate.execute(
                        // 레디스 스크립트
                        RedisScript.of(reserveScript, Boolean.class),
                        // keys
                        List.of(totalKey, reservedKey, reservationKey, reservationBackupKey),
                        // arvgs
                        String.valueOf(reserveAmountMicro),
                        campaignId,
                        String.valueOf(System.currentTimeMillis()),
                        String.valueOf(ttl.getSeconds()))
                .single()
                .defaultIfEmpty(false);
    }

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
