package com.example.ad_manager.redis;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignRedisService {

  private static final String INITIAL_RESERVED_BUDGET = "0";

  private final StringRedisTemplate redisTemplate;
  private final DefaultRedisScript<Long> activateCampaignLuaScript;
  private final CampaignRedisHashMapper campaignRedisHashMapper;

  public void activate(CampaignRedisEntity campaign) {
    redisTemplate.execute(
        activateCampaignLuaScript,
        buildActivationKeys(campaign.getId()),
        buildActivationArgs(campaign)
    );
  }

  public void deactivate(String campaignId) {
    redisTemplate.opsForSet().remove(RedisKeys.CAMPAIGN_LIST_KEY, campaignId);
  }

  private List<String> buildActivationKeys(String campaignId) {
    return List.of(
        RedisKeys.campaignKey(campaignId),
        RedisKeys.campaignTotalBudgetKey(campaignId),
        RedisKeys.campaignReservedBudgetKey(campaignId),
        RedisKeys.CAMPAIGN_LIST_KEY
    );
  }

  private Object[] buildActivationArgs(CampaignRedisEntity campaign) {
    List<String> args = new ArrayList<>();
    args.add(campaign.getId());
    args.add(String.valueOf(campaign.getRemainingBudgetMicro()));
    args.add(INITIAL_RESERVED_BUDGET);
    campaignRedisHashMapper.toHash(campaign).forEach((field, value) -> {
      args.add(field);
      args.add(value);
    });
    return args.toArray();
  }
}
