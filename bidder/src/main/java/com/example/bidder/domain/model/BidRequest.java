package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record BidRequest(
    String id,
    Imp imp,
    Device device,
    User user
) {

}
