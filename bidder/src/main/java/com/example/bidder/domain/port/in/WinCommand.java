package com.example.bidder.domain.port.in;

public record WinCommand(
    String requestId,
    String campaignId,
    String creativeId
) {

}
