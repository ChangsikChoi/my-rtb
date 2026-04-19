package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Bid(
    String auctionId,
    String requestId,
    String campaignId,
    String creativeId,
    Long bidPriceCpmMicro,
    Long receivedAt,
    String adMarkup,
    String winUrl
) {

  public long impressionPriceMicro() {
    return this.bidPriceCpmMicro != null ? this.bidPriceCpmMicro / 1000 : 0L;
  }
}
