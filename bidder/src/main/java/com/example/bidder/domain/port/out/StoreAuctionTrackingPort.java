package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.AuctionTracking;
import reactor.core.publisher.Mono;

public interface StoreAuctionTrackingPort {

  Mono<Void> storeAuctionTracking(AuctionTracking auctionTracking);
}
