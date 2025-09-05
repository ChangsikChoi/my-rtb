package com.example.bidder.domain.port.in;

import com.example.bidder.domain.model.Bid;
import reactor.core.publisher.Mono;

public interface BidUseCase {
    Mono<Bid> handleBidRequest(BidCommand command);
}
