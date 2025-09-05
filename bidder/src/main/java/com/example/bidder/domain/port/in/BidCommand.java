package com.example.bidder.domain.port.in;

import com.example.bidder.domain.model.BidRequest;
import com.example.bidder.domain.model.Device;
import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.model.User;

public record BidCommand(
    String requestId,
    Impression impression,
    Device device,
    User user
) {

  public BidRequest toDomain() {
    return BidRequest.builder()
        .id(this.requestId())
        .impression(this.impression())
        .device(this.device())
        .user(this.user())
        .build();
  }
}
