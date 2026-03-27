package com.example.ad_manager.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TargetCreateRequest(
    String os,
    String country,
    @Min(value = 0, message = "target.minAge must be greater than or equal to 0")
    @Max(value = 150, message = "target.minAge must be less than or equal to 150")
    Integer minAge,
    @Min(value = 0, message = "target.maxAge must be greater than or equal to 0")
    @Max(value = 150, message = "target.maxAge must be less than or equal to 150")
    Integer maxAge
) {

  @AssertTrue(message = "target.minAge must be less than or equal to target.maxAge")
  public boolean isAgeRangeValid() {
    return minAge == null || maxAge == null || minAge <= maxAge;
  }
}
