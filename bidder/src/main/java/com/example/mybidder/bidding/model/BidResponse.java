package com.example.mybidder.bidding.model;

import java.math.BigDecimal;

public record BidResponse(
        String requestId,
        BigDecimal price,
        String adMarkup,
        String winUrl
) {
}
