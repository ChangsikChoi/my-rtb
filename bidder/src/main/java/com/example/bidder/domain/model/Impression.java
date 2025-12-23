package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Impression(
    String id,
    String campaignId,
    String creativeId
) {
}
