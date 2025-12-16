package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Win(
    String id,
    String campaignId,
    String creativeId
) {
}
