package com.example.bidder.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CampaignTest {

  @Nested
  class TimeRangeTest {
    @Test
    void startDate_afterCurrentTime_returnFalse() {
      LocalDateTime now = LocalDateTime.now();
      Campaign campaign = Campaign.builder()
          .startDate(now.plusDays(1))
          .build();

      BidRequest bidRequest = BidRequest.builder().build();

      assertFalse(campaign.isBiddable(bidRequest));

    }

    @Test
    void endDate_beforeCurrentTime_returnFalse() {
      LocalDateTime now = LocalDateTime.now();
      Campaign campaign = Campaign.builder()
          .startDate(now.minusDays(3))
          .endDate(now.minusDays(1))
          .build();

      BidRequest bidRequest = BidRequest.builder().build();

      assertFalse(campaign.isBiddable(bidRequest));
    }
  }

  @Nested
  class BudgetTest {

    @Test
    void remainingBudget_lessThanOrEqualToBidFloor_returnFalse() {
      Campaign campaign = Campaign.builder()
          .startDate(LocalDateTime.now().minusDays(1))
          .endDate(LocalDateTime.now().plusDays(1))
          .remainingBudgetMicro(50_000L)
          .targetCpmMicro(1_000_000L)
          .build();

      BidRequest bidRequest = BidRequest.builder()
          .impression(Impression.builder().bidFloorMicro(50_000L).build())
          .build();

      assertFalse(campaign.isBiddable(bidRequest));
    }

    @Test
    void targetCpm_lessThanOrEqualToBidFloor_returnFalse() {
      Campaign campaign = Campaign.builder()
          .startDate(LocalDateTime.now().minusDays(1))
          .endDate(LocalDateTime.now().plusDays(1))
          .remainingBudgetMicro(1_000_000L)
          .targetCpmMicro(50_000L)
          .build();

      BidRequest bidRequest = BidRequest.builder()
          .impression(Impression.builder().bidFloorMicro(50_000L).build())
          .build();

      assertFalse(campaign.isBiddable(bidRequest));
    }
  }

  @Test
  void allConditionsSatisfied_returnTrue() {
    LocalDateTime now = LocalDateTime.now();
    Campaign campaign = Campaign.builder()
        .startDate(now.minusDays(1))
        .endDate(now.plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(100_000L)
        .build();

    BidRequest bidRequest = BidRequest.builder()
        .impression(Impression.builder().bidFloorMicro(50_000L).build())
        .build();

    assertTrue(campaign.isBiddable(bidRequest));
  }
}