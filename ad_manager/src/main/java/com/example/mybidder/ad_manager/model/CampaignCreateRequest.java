package com.example.mybidder.ad_manager.model;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignCreateRequest(
        String name,
        String region,
        BigDecimal targetCpm,
        BigDecimal budget,
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDateTime startDate,
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDateTime endDate
) {
}
