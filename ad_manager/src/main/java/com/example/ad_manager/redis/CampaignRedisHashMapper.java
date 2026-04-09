package com.example.ad_manager.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.hash.Jackson2HashMapper;
import org.springframework.stereotype.Component;

@Component
public class CampaignRedisHashMapper {

  private final Jackson2HashMapper hashMapper;

  public CampaignRedisHashMapper(ObjectMapper objectMapper) {
    this.hashMapper = new Jackson2HashMapper(objectMapper.copy(), true);
  }

  public Map<String, String> toHash(CampaignRedisEntity campaign) {
    Map<String, String> hash = new LinkedHashMap<>();
    hashMapper.toHash(campaign).forEach(
        (field, value) -> addHashField(hash, String.valueOf(field), value)
    );
    return hash;
  }

  private void addHashField(Map<String, String> hash, String field, Object value) {
    if (value == null) {
      return;
    }
    hash.put(field, String.valueOf(value));
  }
}
