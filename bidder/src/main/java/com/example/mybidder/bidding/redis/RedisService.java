package com.example.mybidder.bidding.redis;

import com.example.mybidder.bidding.model.BidRequest;
import com.example.mybidder.bidding.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Flux<Campaign> getCampaignFlux() {
        return redisTemplate.opsForSet()
                .members(RedisKeys.CAMPAIGN_LIST_KEY)
                .flatMap(id -> redisTemplate.opsForHash()
                        .entries(RedisKeys.campaignKey(id))
                        .collectMap(
                                entry -> entry.getKey().toString(),
                                entry -> entry.getValue().toString()
                        )
                        .map(data -> Campaign.fromRedis(id, data))
                );
    }

    public Mono<Campaign> getHighestCpmCampaign(String region, BigDecimal bidfloor) {
        return getCampaignFlux()
                .filter(campaign ->
                        campaign.hasSufficientBudget() &&
                                campaign.matchRegion(region) &&
                                campaign.targetCpmMicro() >= MicroConverter.convertCpmToMicro(bidfloor)
                )
                .sort(Comparator.comparingLong(Campaign::targetCpmMicro))
                .next();
    }
}
