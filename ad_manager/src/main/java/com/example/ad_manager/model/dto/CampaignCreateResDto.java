package com.example.ad_manager.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CampaignCreateResDto(
    String id,
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    LocalDateTime startDate,
    LocalDateTime endDate,
    TargetCreateResDto target,
    CreativeCreateResDto creative,
    boolean active,
    String owner,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
