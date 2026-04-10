package com.example.ad_manager.mapper.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CreativeEntity;
import com.example.ad_manager.entity.TargetEntity;
import com.example.ad_manager.redis.CampaignRedisEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CampaignRedisProjectionMapperTest {

  private final CampaignRedisProjectionMapper campaignRedisProjectionMapper =
      new CampaignRedisProjectionMapper();

  @Test
  void givenCampaignEntity_whenEntityToRedisEntity_thenMapProjectionAndConvertValues() {
    CampaignEntity entity = mock(CampaignEntity.class);
    TargetEntity target = mock(TargetEntity.class);
    CreativeEntity creative = mock(CreativeEntity.class);

    when(entity.getId()).thenReturn("campaign-1");
    when(entity.getName()).thenReturn("test-campaign-redis");
    when(entity.getTargetCpm()).thenReturn(BigDecimal.valueOf(10.5));
    when(entity.getBudget()).thenReturn(BigDecimal.valueOf(100.0));
    when(entity.getStartDate()).thenReturn(LocalDateTime.of(2026, 3, 23, 0, 0, 0));
    when(entity.getEndDate()).thenReturn(LocalDateTime.of(2026, 3, 24, 23, 59, 59));
    when(entity.getTarget()).thenReturn(target);
    when(entity.getCreative()).thenReturn(creative);

    when(target.getOs()).thenReturn("Android");
    when(target.getCountry()).thenReturn("KR");
    when(target.getMinAge()).thenReturn(20);
    when(target.getMaxAge()).thenReturn(40);

    when(creative.getId()).thenReturn("creative-1");
    when(creative.getImageUrl()).thenReturn("https://example.com/a.png");
    when(creative.getClickUrl()).thenReturn("https://example.com");
    when(creative.getWidth()).thenReturn(300);
    when(creative.getHeight()).thenReturn(250);

    CampaignRedisEntity result = campaignRedisProjectionMapper.entityToRedisEntity(entity);

    assertThat(result.getId()).isEqualTo("campaign-1");
    assertThat(result.getName()).isEqualTo("test-campaign-redis");
    assertThat(result.getTargetCpmMicro()).isEqualTo(10_500_000L);
    assertThat(result.getBudgetMicro()).isEqualTo(100_000_000L);
    assertThat(result.getRemainingBudgetMicro()).isEqualTo(100_000_000L);
    assertThat(result.getStartDate()).isEqualTo("20260323000000");
    assertThat(result.getEndDate()).isEqualTo("20260324235959");
    assertThat(result.getTarget().getOs()).isEqualTo("Android");
    assertThat(result.getTarget().getCountry()).isEqualTo("KR");
    assertThat(result.getTarget().getMinAge()).isEqualTo(20);
    assertThat(result.getTarget().getMaxAge()).isEqualTo(40);
    assertThat(result.getCreative().getId()).isEqualTo("creative-1");
    assertThat(result.getCreative().getImageUrl()).isEqualTo("https://example.com/a.png");
    assertThat(result.getCreative().getClickUrl()).isEqualTo("https://example.com");
    assertThat(result.getCreative().getWidth()).isEqualTo(300);
    assertThat(result.getCreative().getHeight()).isEqualTo(250);
  }
}
