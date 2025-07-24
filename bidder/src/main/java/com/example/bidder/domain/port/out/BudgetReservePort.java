package com.example.bidder.domain.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

public interface BudgetReservePort {

  Mono<Boolean> reserveBudget(String campaignId, String requestId, BigDecimal reserveAmount,
      Duration ttl);
}
