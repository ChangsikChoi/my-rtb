package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Click(
    String id,
    String campaignId,
    String creativeId
) {
}
