package com.example.ad_manager.redis;

import lombok.Builder;

@Builder
public class CreativeRedisEntity {
  private String id;
  private String imageUrl;
  private String clickUrl;
  private Integer width;
  private Integer height;

}
