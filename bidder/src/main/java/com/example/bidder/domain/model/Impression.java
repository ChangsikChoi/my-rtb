package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Impression(
    String auctionId,
    String requestId,
    String campaignId,
    String creativeId,
    Long receivedAt
) {
}
