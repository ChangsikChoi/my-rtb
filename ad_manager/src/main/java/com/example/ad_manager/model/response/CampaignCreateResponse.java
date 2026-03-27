package com.example.ad_manager.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record CampaignCreateResponse(
    String id,
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,
    TargetCreateResponse target,
    CreativeCreateResponse creative,
    boolean active,
    String owner,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {

}
