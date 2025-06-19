package com.example.mybidder.bidding.service;

import com.example.mybidder.bidding.redis.BudgetWithLuaScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImpressionService {

    private final BudgetWithLuaScriptService budgetWithLuaScriptService;

    public Mono<Boolean> handleImpression(String requestId) {
        return budgetWithLuaScriptService.confirmBudget(requestId);
    }
}
