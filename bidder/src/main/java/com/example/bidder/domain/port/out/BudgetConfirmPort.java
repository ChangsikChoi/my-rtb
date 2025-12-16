package com.example.bidder.domain.port.out;

import reactor.core.publisher.Mono;

public interface BudgetConfirmPort {

  Mono<Boolean> confirmBudget(String requestId, String campaignId);
}
