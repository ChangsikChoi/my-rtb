package com.example.ad_manager.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CampaignCreateReqDto(
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    LocalDateTime startDate,
    LocalDateTime endDate,
    TargetCreateReqDto target,
    CreativeCreateReqDto creative,
    boolean active,
    String owner
) {

}
