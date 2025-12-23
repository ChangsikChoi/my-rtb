package com.example.bidder.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record Campaign(
    String id,
    String name,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Long targetCpmMicro,
    Long budgetMicro,
    Long remainingBudgetMicro,
    Target target,
    Creative creative
) {
  public boolean isBiddable(BidRequest bidRequest) {
    LocalDateTime now = LocalDateTime.now();
    Imp imp = bidRequest.imp();
    long reqBidFloor = imp != null ? imp.bidFloorMicro() : 0L;

    long remainingBudget = this.remainingBudgetMicro != null ? this.remainingBudgetMicro : 0L;
    long targetCpm = this.targetCpmMicro != null ? this.targetCpmMicro : 0L;

    if (this.startDate.isAfter(now) || this.endDate.isBefore(now)) {
      return false;
    }
    //캠페인 남은 예산 확인 (입찰가보다 작거나 같으면 제외)
    if (remainingBudget <= reqBidFloor) {
      return false;
    }
    //켐페인 목표 CPM 확인
    if (targetCpm <= reqBidFloor) {
      return false;
    }
    return true;
  }
}
