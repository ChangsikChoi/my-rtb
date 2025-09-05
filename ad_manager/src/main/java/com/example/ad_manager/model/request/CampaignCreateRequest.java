package com.example.ad_manager.model.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignCreateRequest(
    String name,
    BigDecimal targetCpm,
    BigDecimal budget,
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDateTime startDate,
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDateTime endDate,
    TargetCreateRequest target,
    CreativeCreateRequest creative
) {

}
