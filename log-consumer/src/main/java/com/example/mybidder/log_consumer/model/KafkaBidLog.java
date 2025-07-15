package com.example.mybidder.log_consumer.model;


public record KafkaBidLog(
        String requestId,
        String campaignId,
        double price) {
}
