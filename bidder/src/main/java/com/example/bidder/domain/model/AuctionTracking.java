package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record AuctionTracking(
    String auctionId,
    String requestId,
    String campaignId,
    String creativeId,
    Long priceMicro,
    Long receivedAt
) {
}
