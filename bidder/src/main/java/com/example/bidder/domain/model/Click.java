package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Click(
    String auctionId,
    String requestId,
    String campaignId,
    String creativeId
) {

  public Click(String requestId, String campaignId, String creativeId) {
    this(null, requestId, campaignId, creativeId);
  }

  public String id() {
    return requestId;
  }
}
