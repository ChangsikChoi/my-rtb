package com.example.bidder.domain.port.in;

import java.math.BigDecimal;

public record BidCommand(
    String requestId,
    String region,
    BigDecimal bidfloor
) {}
