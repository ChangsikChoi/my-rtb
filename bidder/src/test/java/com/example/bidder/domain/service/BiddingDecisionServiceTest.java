package com.example.bidder.domain.service;

import com.example.bidder.domain.model.BidRequest;
import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.model.Creative;
import com.example.bidder.domain.model.Imp;
import com.example.bidder.domain.model.Target;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class BiddingDecisionServiceTest {

  private final BiddingDecisionService service = new BiddingDecisionService();

  @Test
  void selectWinner_noEligibleCampaigns_returnEmpty() {
    BidRequest bidRequest = BidRequest.builder().build();

    Campaign campaignA = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(3))
        .endDate(LocalDateTime.now().minusDays(1))
        .build();

    Campaign campaignB = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(3))
        .endDate(LocalDateTime.now().minusDays(1))
        .build();

    StepVerifier.create(service.selectWinner(Flux.just(campaignA, campaignB), bidRequest))
        .verifyComplete();
  }

  @Test
  void selectWinner_multiEligibleCampaigns_returnHighestCpmCampaign() {
    BidRequest bidRequest = BidRequest.builder()
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .bidFloorMicro(50_000L)
            .build())
        .build();

    Target target = Target.builder().build();
    Creative creative = Creative.builder()
        .width(300)
        .height(250)
        .build();

    Campaign campaignA = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(100_000L)
        .target(target)
        .creative(creative)
        .build();

    Campaign campaignB = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(200_000L)
        .target(target)
        .creative(creative)
        .build();

    Campaign campaignC = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(150_000L)
        .target(target)
        .creative(creative)
        .build();

    StepVerifier.create(
            service.selectWinner(Flux.just(campaignA, campaignB, campaignC), bidRequest))
        .expectNext(campaignB)
        .verifyComplete();
  }

  @Test
  void selectWinner_singleEligibleCampaign_returnThatCampaign() {
    BidRequest bidRequest = BidRequest.builder()
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .bidFloorMicro(50_000L)
            .build())
        .build();

    Target target = Target.builder().build();
    Creative creative = Creative.builder()
        .width(300)
        .height(250)
        .build();

    Campaign campaignA = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(100_000L)
        .target(target)
        .creative(creative)
        .build();
    Campaign campaignB = Campaign.builder()
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(10_000L)
        .target(target)
        .creative(creative)
        .build();

    StepVerifier.create(service.selectWinner(Flux.just(campaignA, campaignB), bidRequest))
        .expectNext(campaignA)
        .verifyComplete();
  }
}