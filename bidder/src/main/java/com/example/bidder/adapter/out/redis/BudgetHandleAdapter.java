package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.BudgetConfirmPort;
import com.example.bidder.domain.port.out.BudgetReservePort;
import com.example.bidder.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BudgetHandleAdapter implements BudgetReservePort, BudgetConfirmPort {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> reserveBudget(String campaignId, String requestId, BigDecimal reserveAmount, Duration ttl) {
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
                        String.valueOf(MicroConverter.convertCpmToMicro(reserveAmount) / 1000),
                        campaignId,
                        String.valueOf(System.currentTimeMillis()),
                        String.valueOf(ttl.getSeconds()))
                .single()
                .defaultIfEmpty(false);
    }

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
