package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.model.Campaign;
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
        // 레디스 등록 캠페인 목록 조회
        return redisTemplate.opsForSet()
                .members(RedisKeys.CAMPAIGN_LIST_KEY)
                // 레디스 캠페인 ID 목록으로 개별 캠페인 정보 조회
                .flatMap(id -> redisTemplate.opsForHash()
                        .entries(RedisKeys.campaignKey(id))
                        .collectMap(
                                entry -> entry.getKey().toString(),
                                entry -> entry.getValue().toString()
                        )
                        .map(data -> CampaignEntity.fromRedis(id, data))
                )
                // 캠페인 필터링 (예산, 지역 및 입찰가 기준)
                .filter(campaign ->
                        campaign.hasSufficientBudget() &&
                                campaign.matchRegion(region) &&
                                campaign.targetCpmMicro() >= MicroConverter.convertCpmToMicro(bidfloor)
                )
                // 캠페인 정렬 (목표 CPM 높은 순)
                .sort(Comparator.comparingLong(CampaignEntity::targetCpmMicro))
                // 첫 번째 캠페인 선택 및 도메인 모델로 변환
                .next()
                .map(CampaignEntity::toDomain);
    }
}
