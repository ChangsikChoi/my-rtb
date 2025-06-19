package com.example.mybidder.ad_manager.model;


import com.example.mybidder.ad_manager.entity.CampaignEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignCreateResponse(
        String id,
        String name,
        String region,
        BigDecimal targetCpm,
        BigDecimal budget,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean active,
        String owner,
        LocalDateTime createdAt
) {
    public static CampaignCreateResponse fromEntity(CampaignEntity entity) {
        return new CampaignCreateResponse(
                entity.getId(),
                entity.getName(),
                entity.getRegion(),
                entity.getTargetCpm(),
                entity.getBudget(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.isActive(),
                entity.getOwner(),
                entity.getCreatedAt()
        );
    }
}
