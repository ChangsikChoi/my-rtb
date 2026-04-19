package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.LoadClickUrlPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CampaignClickUrlAdapter implements LoadClickUrlPort {

  private static final String CREATIVE_ID_FIELD = "creative.id";
  private static final String CREATIVE_CLICK_URL_FIELD = "creative.clickUrl";

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Mono<String> loadClickUrl(String campaignId, String creativeId) {
    return redisTemplate.opsForHash()
        .multiGet(
            RedisKeys.campaignKey(campaignId),
            List.of(CREATIVE_ID_FIELD, CREATIVE_CLICK_URL_FIELD)
        )
        .filter(values -> values.size() == 2)
        .filter(values -> creativeId.equals(values.get(0)))
        .map(values -> (String) values.get(1))
        .filter(clickUrl -> clickUrl != null && !clickUrl.isBlank());
  }
}
