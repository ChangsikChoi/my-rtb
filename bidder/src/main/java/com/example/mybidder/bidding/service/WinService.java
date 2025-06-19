package com.example.mybidder.bidding.service;

import com.example.mybidder.bidding.redis.BudgetWithLuaScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WinService {

    private final BudgetWithLuaScriptService budgetWithLuaScriptService;

    public void win(String requestId) {
        // 입찰 승리 로깅
    }
}
