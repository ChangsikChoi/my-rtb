package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Impression(
    Long bidFloorMicro,
    String placementId,
    Integer width,
    Integer height
) {

}