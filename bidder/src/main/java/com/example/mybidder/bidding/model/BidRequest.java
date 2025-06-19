package com.example.mybidder.bidding.model;

import java.math.BigDecimal;

public record BidRequest (
        String requestId,
        String region,
        BigDecimal bidfloor
) {}