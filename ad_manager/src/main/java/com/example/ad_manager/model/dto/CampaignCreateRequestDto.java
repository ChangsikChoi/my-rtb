package com.example.ad_manager.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CampaignCreateRequestDto(
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    LocalDateTime startDate,
    LocalDateTime endDate,
    TargetCreateRequestDto target,
    CreativeCreateRequestDto creative
) {

}
