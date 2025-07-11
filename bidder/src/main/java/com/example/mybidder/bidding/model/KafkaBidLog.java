package com.example.mybidder.bidding.model;

import java.math.BigDecimal;

public class KafkaBidLog {
    private String requestId;
    private String campaignId;
    private double price;

    public KafkaBidLog(String s, String id, BigDecimal bigDecimal) {
        this.requestId = s;
        this.campaignId = id;
        this.price = bigDecimal.doubleValue();
    }
}
