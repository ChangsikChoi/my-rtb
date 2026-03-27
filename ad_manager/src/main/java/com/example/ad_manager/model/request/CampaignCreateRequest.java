package com.example.ad_manager.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignCreateRequest(
    @NotBlank(message = "name is required")
    String name,
    @NotNull(message = "targetCpm is required")
    @Positive(message = "targetCpm must be positive")
    BigDecimal targetCpm,
    @NotNull(message = "budget is required")
    @Positive(message = "budget must be positive")
    BigDecimal budget,
    @NotNull(message = "startDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,
    @NotNull(message = "endDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,
    @NotNull(message = "target is required")
    @Valid
    TargetCreateRequest target,
    @NotNull(message = "creative is required")
    @Valid
    CreativeCreateRequest creative
) {

  @AssertTrue(message = "startDate must be before or equal to endDate")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || !startDate.isAfter(endDate);
  }
}
