package com.example.mybidder.bidding.redis;

import lombok.Getter;

@Getter
public enum LuaScripts {
    CAMPAIGN_BUDGET_RESERVE("reserve_budget.lua"),
    CAMPAIGN_BUDGET_CONFIRM("confirm_budget.lua"),
    CAMPAIGN_BUDGET_REFUND("refund_budget.lua");

    private final String scriptName;

    LuaScripts(String scriptName) {
        this.scriptName = scriptName;
    }

    public String loadScript() {
        return LuaScriptLoader.loadScript(scriptName);
    }
}
