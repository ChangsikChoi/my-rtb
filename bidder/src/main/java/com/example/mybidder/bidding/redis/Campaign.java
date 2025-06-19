package com.example.mybidder.bidding.redis;

import java.util.Map;

public record Campaign(
        String id,
        String name,
        String region,
        long targetCpmMicro,
        long budgetMicro,
        long remainingBudgetMicro
) {

    public static Campaign fromRedis(String id, Map<String, String> data) {
        return new Campaign(
                id,
                data.get("name"),
                data.get("region"),
                Long.parseLong(data.get("targetCpmMicro")),
                Long.parseLong(data.get("budgetMicro")),
                Long.parseLong(data.get("remainingBudgetMicro"))
        );
    }

    public boolean matchRegion(String reqRegion) {
        return this.region.equalsIgnoreCase(reqRegion);
    }

    public boolean hasSufficientBudget() {
        return this.remainingBudgetMicro > 0;
    }
}
