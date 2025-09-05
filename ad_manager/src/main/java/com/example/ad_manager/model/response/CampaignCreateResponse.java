package com.example.ad_manager.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CampaignCreateResponse(
    String id,
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    LocalDateTime startDate,
    LocalDateTime endDate,
    TargetCreateResponse target,
    CreativeCreateResponse creative,
    boolean active,
    String owner,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
