package com.example.bidder.adapter.in.web;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BidRequestDto {
    private String requestId;
    private String region;
    private BigDecimal bidfloor;
}
