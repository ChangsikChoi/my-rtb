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
    Device device = bidRequest.device();
    User user = bidRequest.user();

    String requestOs = device != null ? device.os() : null;
    String requestCountry = device != null ? device.country() : null;
    Gender requestGender = user != null ? user.gender() : null;
    Integer requestAge = user != null ? user.age() : null;

    if (isExcludedByOs(requestOs)) {
      return false;
    }
    if (isExcludedByCountry(requestCountry)) {
      return false;
    }
    if (isExcludedByGender(requestGender)) {
      return false;
    }
    if (isExcludedByAge(requestAge)) {
      return false;
    }

    return true;
  }

  private boolean isExcludedByAge(Integer age) {

    if (age == null) {
      // 요청에 나이가 없으면 타겟에 나이 제한이 존재하는 경우 제외
      return this.minAge != null || this.maxAge != null;
    }
    if (this.minAge != null && age < this.minAge) {
      return true;
    }
    if (this.maxAge != null && age > this.maxAge) {
      return true;
    }
    return false;
  }

  private boolean isExcludedByGender(Gender gender) {
    return this.gender != null
        && this.gender != gender;
  }

  private boolean isExcludedByCountry(String country) {
    return this.country != null
        && !this.country.equals(country);
  }

  private boolean isExcludedByOs(String os) {
    return this.os != null
        && !this.os.equals(os);
  }
}
