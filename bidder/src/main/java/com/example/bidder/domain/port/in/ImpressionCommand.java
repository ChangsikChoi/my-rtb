package com.example.bidder.domain.port.in;

public record ImpressionCommand(
    String requestId,
    String campaignId,
    String creativeId
) {

}
