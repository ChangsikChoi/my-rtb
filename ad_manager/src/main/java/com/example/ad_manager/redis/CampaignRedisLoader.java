package com.example.ad_manager.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CampaignRedisLoader {

    private final StringRedisTemplate redisTemplate;

    public void load(Campaign campaign) {
        String key = RedisKeys.campaignKey(campaign.id());
        Map<String, String> data = new HashMap<>();
        data.put("targetCpmMicro", String.valueOf(campaign.targetCpmMicro()));
        data.put("budgetMicro", String.valueOf(campaign.budgetMicro()));
        data.put("remainingBudgetMicro", String.valueOf(campaign.remainingBudgetMicro()));
        data.put("region", campaign.region());
        data.put("name", campaign.name());

        redisTemplate.opsForHash().putAll(key, data);

        String totalKey = RedisKeys.campaignTotalBudgetKey(campaign.id());
        String reservedKey = RedisKeys.campaignReservedBudgetKey(campaign.id());

        redisTemplate.opsForValue().set(totalKey, String.valueOf(campaign.remainingBudgetMicro()));
        redisTemplate.opsForValue().set(reservedKey, String.valueOf(0));

        // TODO: 활성상태에 따른 목록 관리 로직 추가 시점에 이동
        // 캠페인 ID 세트
        redisTemplate.opsForSet().add(RedisKeys.CAMPAIGN_LIST_KEY, campaign.id());
    }
}
