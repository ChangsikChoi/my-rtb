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
    if (this.startDate.isAfter(now) || this.endDate.isBefore(now)) {
      return false;
    }
    //캠페인 남은 예산 확인
    if (this.remainingBudgetMicro <= bidRequest.impression().bidFloorMicro()) {
      return false;
    }
    //켐페인 목표 CPM 확인
    if (this.targetCpmMicro >= bidRequest.impression().bidFloorMicro()) {
      return false;
    }
    return true;
  }
}
