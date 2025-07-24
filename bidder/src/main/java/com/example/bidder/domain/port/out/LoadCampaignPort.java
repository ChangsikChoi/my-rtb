package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.Campaign;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface LoadCampaignPort {
    Mono<Campaign> loadCampaign(String region, BigDecimal bidfloor);
}
