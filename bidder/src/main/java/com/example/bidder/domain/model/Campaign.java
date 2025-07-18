package com.example.bidder.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Campaign {
    private final String id;
    private final String name;
    private final String region;
    private final BigDecimal targetCpm;
    private final BigDecimal budget;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final boolean active;
    private final String owner;
}
