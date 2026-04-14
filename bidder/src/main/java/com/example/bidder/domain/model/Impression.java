package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Impression(
    String auctionId,
    String requestId,
    String campaignId,
    String creativeId
) {

  public Impression(String requestId, String campaignId, String creativeId) {
    this(null, requestId, campaignId, creativeId);
  }

  public String id() {
    return requestId;
  }
}
