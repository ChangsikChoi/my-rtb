package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Creative (
    String id,
    Integer width,
    Integer height,
    String imageUrl,
    String clickUrl
){
  public boolean isSizeMatched(BidRequest bidRequest) {
    // 비딩 요청 너비 비교
    if (bidRequest.impression().width() != null) {
      if (this.width == null) {
        return false;
      }
      if (!bidRequest.impression().width().equals(this.width)) {
        return false;
      }
    } else if (this.width != null) {
      // 캠페인 크리에이티브 너비가 있지만 비딩 요청 너비가 없는 경우
      return false;
    }
    // 비딩 요청 높이 비교
    if (bidRequest.impression().height() != null) {
      if (this.height == null) {
        return false;
      }
      if (!bidRequest.impression().height().equals(this.height)) {
        return false;
      }
    } else if (this.height != null) {
      // 캠페인 크리에이티브 높이가 있지만 비딩 요청 높이가 없는 경우
      return false;
    }
    return true;
  }
}
