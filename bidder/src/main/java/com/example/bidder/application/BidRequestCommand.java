package com.example.bidder.application;

import java.math.BigDecimal;

public record BidRequestCommand(
        String requestId,
        String region,
        BigDecimal bidfloor
) {}