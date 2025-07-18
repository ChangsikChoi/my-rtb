package com.example.bidder.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class BidRequest {

    private final String id;
    private final String campaignId;
    private final String region;
    private final BigDecimal bidfloor;

    // TODO: 요청 객체 값 검증 로직 등 추가.
}
