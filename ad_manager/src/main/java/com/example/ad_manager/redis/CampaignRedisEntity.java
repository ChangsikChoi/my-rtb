package com.example.ad_manager.redis;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "campaign")
@Builder
@Getter
public class CampaignRedisEntity {

  @Id
  private String id;
  private String name;
  private long targetCpmMicro;
  private long budgetMicro;
  private long remainingBudgetMicro;
  private String startDate;
  private String endDate;
  private TargetRedisEntity target;
  private CreativeRedisEntity creative;

}
