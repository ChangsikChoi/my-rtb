package com.example.bidder.application;

import java.math.BigDecimal;

public record BidResponseVo(
        String requestId,
        BigDecimal price,
        String adMarkup,
        String winUrl
) {
}
