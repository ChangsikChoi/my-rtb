package com.example.bidder.domain.port.out;

import reactor.core.publisher.Mono;

public interface BudgetReservePort {

  Mono<Boolean> reserveBudget(String campaignId, String requestId, Long reserveAmountMicro);
}
