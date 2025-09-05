package com.example.bidder.domain.model;

import lombok.Builder;

@Builder
public record Target(
    String os,
    String country,
    Gender gender,
    Integer minAge,
    Integer maxAge
) {

  public boolean isTargeted(BidRequest bidRequest) {
    // os 타게팅 확인
    if (this.os != null
        && !this.os.equals(bidRequest.device().os())) {
      return false;
    }
    // 국가 타게팅 확인
    if (this.country != null
        && !this.country.equals(bidRequest.device().country())) {
      return false;
    }
    // 성별 타게팅 확인
    if (this.gender != bidRequest.user().gender()) {
      return false;
    }
    // 나이 타게팅 확인
    // 최소 나이 확인
    if (this.minAge != null) {
      if (bidRequest.user().age() == null) {
        return false;
      } else if (this.minAge > bidRequest.user().age()) {
        return false;
      }
    }
    // 최대 나이 확인
    if (this.maxAge != null) {
      if (bidRequest.user().age() == null) {
        return false;
      } else if (this.maxAge < bidRequest.user().age()) {
        return false;
      }
    }

    return true;
  }
}
