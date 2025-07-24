package com.example.bidder.domain.model;

import java.math.BigDecimal;

public record Bid(
    String requestId,
    String campaignId,
    BigDecimal price,
    String adMarkup,
    String winUrl
) {
}
