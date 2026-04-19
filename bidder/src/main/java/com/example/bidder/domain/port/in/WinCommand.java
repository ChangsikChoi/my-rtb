package com.example.bidder.domain.port.in;

public record WinCommand(
    String auctionId,
    long receivedAt
) {

}
