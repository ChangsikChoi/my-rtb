package com.example.mybidder.ad_manager.model;

import com.example.mybidder.ad_manager.entity.CampaignEntity;
import com.example.mybidder.ad_manager.redis.Campaign;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignCreateReqDto(
        String name,
        String region,
        BigDecimal targetCpm,
        BigDecimal budget,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean active,
        String owner
) {
    public static CampaignCreateReqDto requestToDto(CampaignCreateRequest request, boolean active, String userName) {
        return new CampaignCreateReqDto(
                request.name(),
                request.region(),
                request.targetCpm(),
                request.budget(),
                request.startDate(),
                request.endDate(),
                active,
                userName
        );
    }

    public CampaignEntity toEntity() {
        return CampaignEntity.builder()
                .name(this.name())
                .region(this.region())
                .targetCpm(this.targetCpm())
                .budget(this.budget())
                .startDate(this.startDate())
                .endDate(this.endDate())
                .active(this.active())
                .owner(this.owner())
                .build();
    }

    public Campaign toRedis(String campaignId) {
        return new Campaign(
                campaignId,
                this.name(),
                this.region(),
                this.targetCpm().multiply(BigDecimal.valueOf(1_000_000)).longValue(),
                this.budget().multiply(BigDecimal.valueOf(1_000_000)).longValue(),
                this.budget().multiply(BigDecimal.valueOf(1_000_000)).longValue()
        );
    }
}
