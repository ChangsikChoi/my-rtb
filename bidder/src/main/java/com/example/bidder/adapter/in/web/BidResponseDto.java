package com.example.bidder.adapter.in.web;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BidResponseDto {
    private String requestId;
    private BigDecimal price;
    private String adMarkup;
    private String winUrl;
}
