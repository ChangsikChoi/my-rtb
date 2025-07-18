package com.example.bidder.application.service;

import com.example.bidder.adapter.out.redis.BudgetWithLuaScriptService;
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
