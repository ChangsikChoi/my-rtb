package com.example.bidder.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record Campaign(String id, String name, String region, BigDecimal targetCpm, BigDecimal budget) {
//    private final LocalDateTime startDate;
//    private final LocalDateTime endDate;
//    private final boolean active;
}
