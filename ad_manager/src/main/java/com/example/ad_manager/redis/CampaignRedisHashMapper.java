package com.example.ad_manager.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.hash.Jackson2HashMapper;
import org.springframework.stereotype.Component;

@Component
public class CampaignRedisHashMapper {

  private final Jackson2HashMapper hashMapper;

  public CampaignRedisHashMapper(ObjectMapper objectMapper) {
    this.hashMapper = new Jackson2HashMapper(objectMapper.copy(), true);
  }

  public List<String> toHashArgs(CampaignRedisEntity campaign) {
    List<String> hashArgs = new ArrayList<>();
    hashMapper.toHash(campaign).forEach((field, value) -> addHashField(hashArgs, field, value));
    return hashArgs;
  }

  private void addHashField(List<String> hashArgs, String field, Object value) {
    if (value == null) {
      return;
    }
    hashArgs.add(field);
    hashArgs.add(String.valueOf(value));
  }
}
