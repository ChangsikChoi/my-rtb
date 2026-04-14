package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Win(
    String auctionId,
    String requestId,
    String campaignId,
    String creativeId
) {

  public Win(String requestId, String campaignId, String creativeId) {
    this(null, requestId, campaignId, creativeId);
  }

  public String id() {
    return requestId;
  }
}
