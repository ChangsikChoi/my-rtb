package com.example.bidder.domain.port.in;

public record ImpressionCommand(
    String auctionId,
    long receivedAt
) {

}
