package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Device(
    String ip,
    String country,
    String os
) {

}