package com.example.bidder.domain.service;

import com.example.bidder.domain.model.BidRequest;
import com.example.bidder.domain.model.Campaign;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BiddingDecisionService {

  public Mono<Campaign> selectWinner(Flux<Campaign> campaignFlux, BidRequest bidRequest) {
    return campaignFlux
        .filter(campaign -> isCampaignEligible(campaign, bidRequest))
        .sort((c1, c2) -> Long.compare(c2.targetCpmMicro(), c1.targetCpmMicro()))
        .next();
  }

  private boolean isCampaignEligible(Campaign campaign, BidRequest bidRequest) {
    //캠페인 기간 및 예산 확인
    if (!campaign.isBiddable(bidRequest)) {
      return false;
    }
    //캠페인 타겟팅 확인
    if (!campaign.target().isTargeted(bidRequest)) {
      return false;
    }
    //캠페인 소재 사이즈 매칭 확인
    if (!campaign.creative().isSizeMatched(bidRequest)) {
      return false;
    }

    return true;
  }
}
