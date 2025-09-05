package com.example.bidder.adapter.in.web;

import java.math.BigDecimal;

public record BidRequestDto(
    String id,
    ImpressionDto imp,
    DeviceDto device,
    UserDto user
) {

  public record ImpressionDto(
      BigDecimal bidFloor,
      String placementId,
      Integer width,
      Integer height
  ) {

  }

  public record DeviceDto(
      String ip,
      String country,
      String os
  ) {

  }

  public record UserDto(
      String gender,
      Integer age
  ) {

  }

}
