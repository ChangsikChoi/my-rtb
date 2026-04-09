package com.example.ad_manager.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CampaignResponseDto(
    String id,
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    LocalDateTime startDate,
    LocalDateTime endDate,
    TargetResponseDto target,
    CreativeResponseDto creative,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
