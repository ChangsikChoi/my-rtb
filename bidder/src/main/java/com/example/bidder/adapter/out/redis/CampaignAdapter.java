package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.port.out.LoadCampaignPort;
import com.example.bidder.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class CampaignAdapter implements LoadCampaignPort {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Campaign> loadCampaign(String region, BigDecimal bidfloor) {
        return redisTemplate.opsForSet()
                .members(RedisKeys.CAMPAIGN_LIST_KEY)
                .flatMap(id -> redisTemplate.opsForHash()
                        .entries(RedisKeys.campaignKey(id))
                        .collectMap(
                                entry -> entry.getKey().toString(),
                                entry -> entry.getValue().toString()
                        )
                        .map(data -> Campaign.fromRedis(id, data))
                )
                .filter(campaign ->
                        campaign.hasSufficientBudget() &&
                                campaign.matchRegion(region) &&
                                campaign.targetCpmMicro() >= MicroConverter.convertCpmToMicro(bidfloor)
                )
                .sort(Comparator.comparingLong(Campaign::targetCpmMicro))
                .next();
    }
}
