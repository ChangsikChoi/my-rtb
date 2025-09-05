package com.example.bidder.domain.model;

public record Impression(
    Long bidFloorMicro,
    String placementId,
    Integer width,
    Integer height
) {

}