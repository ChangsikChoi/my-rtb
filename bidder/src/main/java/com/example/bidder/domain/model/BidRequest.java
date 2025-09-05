package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record BidRequest(
    String id,
    Impression impression,
    Device device,
    User user
) {

}
