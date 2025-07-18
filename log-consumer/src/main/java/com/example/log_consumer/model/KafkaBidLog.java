package com.example.log_consumer.model;


public record KafkaBidLog(
        String requestId,
        String campaignId,
        double price) {
}
