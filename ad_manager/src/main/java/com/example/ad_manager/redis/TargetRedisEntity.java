package com.example.ad_manager.redis;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TargetRedisEntity {
  private String os;
  private String country;
  private Integer minAge;
  private Integer maxAge;
}
