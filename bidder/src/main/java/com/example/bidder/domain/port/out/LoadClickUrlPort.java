package com.example.bidder.domain.port.out;

import reactor.core.publisher.Mono;

public interface LoadClickUrlPort {

  Mono<String> loadClickUrl(String campaignId, String creativeId);
}
