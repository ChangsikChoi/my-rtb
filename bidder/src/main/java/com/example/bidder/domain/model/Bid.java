package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Bid(
    String requestId,
    String campaignId,
    String creativeId,
    Long bidPriceCpmMicro,
    String adMarkup,
    String winUrl
) {
}
