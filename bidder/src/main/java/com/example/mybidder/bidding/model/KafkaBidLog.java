package com.example.mybidder.bidding.model;

public record KafkaBidLog(
        String requestId,
        String campaignId,
        double price) {
}
