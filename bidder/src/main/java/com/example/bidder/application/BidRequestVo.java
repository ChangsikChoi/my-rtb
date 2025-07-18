package com.example.bidder.application;

import java.math.BigDecimal;

public record BidRequestVo(
        String requestId,
        String region,
        BigDecimal bidfloor
) {}