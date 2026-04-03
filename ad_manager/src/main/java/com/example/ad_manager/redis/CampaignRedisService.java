package com.example.ad_manager.redis;

import com.example.ad_manager.repository.CampaignRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampaignRedisService {

  private final StringRedisTemplate redisTemplate;
  private final CampaignRedisRepository campaignRedisRepository;

  public void activate(CampaignRedisEntity campaign) {
    campaignRedisRepository.save(campaign);

    String totalKey = RedisKeys.campaignTotalBudgetKey(campaign.getId());
    String reservedKey = RedisKeys.campaignReservedBudgetKey(campaign.getId());

    // 재활성화 시 기존 예산 유지
    redisTemplate.opsForValue().setIfAbsent(totalKey, String.valueOf(campaign.getRemainingBudgetMicro()));
    redisTemplate.opsForValue().setIfAbsent(reservedKey, String.valueOf(0));

    redisTemplate.opsForSet().add(RedisKeys.CAMPAIGN_LIST_KEY, campaign.getId());
  }

  public void deactivate(String campaignId) {
    redisTemplate.opsForSet().remove(RedisKeys.CAMPAIGN_LIST_KEY, campaignId);
  }
}
