package com.example.ad_manager.redis;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreativeRedisEntity {
  private String id;
  private String imageUrl;
  private String clickUrl;
  private Integer width;
  private Integer height;

}
