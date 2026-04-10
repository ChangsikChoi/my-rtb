package com.example.ad_manager.mapper.redis;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CreativeRedisEntity;
import com.example.ad_manager.redis.TargetRedisEntity;
import com.example.ad_manager.utils.MicroConverter;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class CampaignRedisProjectionMapper {

  public CampaignRedisEntity entityToRedisEntity(CampaignEntity entity) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    return CampaignRedisEntity.builder()
        .id(entity.getId())
        .name(entity.getName())
        .targetCpmMicro(MicroConverter.toMicro(entity.getTargetCpm()))
        .budgetMicro(MicroConverter.toMicro(entity.getBudget()))
        .remainingBudgetMicro(MicroConverter.toMicro(entity.getBudget()))
        .startDate(entity.getStartDate().format(dateTimeFormatter))
        .endDate(entity.getEndDate().format(dateTimeFormatter))
        .target(TargetRedisEntity.builder()
            .os(entity.getTarget().getOs())
            .country(entity.getTarget().getCountry())
            .minAge(entity.getTarget().getMinAge())
            .maxAge(entity.getTarget().getMaxAge())
            .build())
        .creative(CreativeRedisEntity.builder()
            .id(entity.getCreative().getId())
            .imageUrl(entity.getCreative().getImageUrl())
            .clickUrl(entity.getCreative().getClickUrl())
            .width(entity.getCreative().getWidth())
            .height(entity.getCreative().getHeight())
            .build())
        .build();
  }
}
