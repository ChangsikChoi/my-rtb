package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.model.Campaign;
import com.example.bidder.utils.MicroConverter;

import java.util.Map;

public record CampaignEntity(
        String id,
        String name,
        String region,
        long targetCpmMicro,
        long budgetMicro,
        long remainingBudgetMicro
) {

    public static CampaignEntity fromRedis(String id, Map<String, String> data) {
        return new CampaignEntity(
                id,
                data.get("name"),
                data.get("region"),
                Long.parseLong(data.get("targetCpmMicro")),
                Long.parseLong(data.get("budgetMicro")),
                Long.parseLong(data.get("remainingBudgetMicro"))
        );
    }

    public Campaign toDomain() {
        return Campaign.builder()
                .id(this.id())
                .name(this.name())
                .region(this.region())
                .targetCpm(MicroConverter.convertMicroToCpm(this.targetCpmMicro()))
                .budget(MicroConverter.convertMicroToCpm(this.budgetMicro()))
                .build();
    }

    public boolean matchRegion(String reqRegion) {
        return this.region.equalsIgnoreCase(reqRegion);
    }

    public boolean hasSufficientBudget() {
        return this.remainingBudgetMicro > 0;
    }
}
