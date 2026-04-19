package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.AuctionTracking;
import reactor.core.publisher.Mono;

public interface LoadAuctionTrackingPort {

  Mono<AuctionTracking> loadAuctionTracking(String auctionId);
}
