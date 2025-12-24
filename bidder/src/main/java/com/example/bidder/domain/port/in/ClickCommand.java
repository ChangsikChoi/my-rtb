package com.example.bidder.domain.port.in;

public record ClickCommand(
    String requestId,
    String campaignId,
    String creativeId
) {

}
