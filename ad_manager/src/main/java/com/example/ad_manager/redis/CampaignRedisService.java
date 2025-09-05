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

  public void save(CampaignRedisEntity campaign) {
    campaignRedisRepository.save(campaign);

    String totalKey = RedisKeys.campaignTotalBudgetKey(campaign.getId());
    String reservedKey = RedisKeys.campaignReservedBudgetKey(campaign.getId());

    redisTemplate.opsForValue().set(totalKey, String.valueOf(campaign.getRemainingBudgetMicro()));
    redisTemplate.opsForValue().set(reservedKey, String.valueOf(0));

    // TODO: 활성상태에 따른 목록 관리 로직 추가 시점에 이동
    // 캠페인 ID 세트
    redisTemplate.opsForSet().add(RedisKeys.CAMPAIGN_LIST_KEY, campaign.getId());
  }


}
