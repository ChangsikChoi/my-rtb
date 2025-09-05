package com.example.bidder.domain.port.out;

import com.example.bidder.domain.model.Campaign;
import reactor.core.publisher.Flux;

public interface LoadCampaignPort {
    Flux<Campaign> loadCampaign();
}
