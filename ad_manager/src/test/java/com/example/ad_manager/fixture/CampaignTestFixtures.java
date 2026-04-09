package com.example.ad_manager.fixture;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CreativeEntity;
import com.example.ad_manager.entity.TargetEntity;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CreativeCreateRequestDto;
import com.example.ad_manager.model.dto.TargetCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.dto.CreativeResponseDto;
import com.example.ad_manager.model.dto.TargetResponseDto;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CreativeRedisEntity;
import com.example.ad_manager.redis.TargetRedisEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class CampaignTestFixtures {

  private static final BigDecimal DEFAULT_TARGET_CPM = BigDecimal.valueOf(10.5);
  private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(100.0);
  private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.of(2026, 3, 23, 0, 0, 0);
  private static final LocalDateTime DEFAULT_END_DATE = LocalDateTime.of(2026, 3, 24, 23, 59, 59);
  private static final LocalDateTime DEFAULT_TIMESTAMP = LocalDateTime.of(2026, 3, 22, 10, 0, 0);

  private CampaignTestFixtures() {
  }

  public static CampaignEntity campaignEntity(String campaignId, boolean active) {
    return campaignEntity(campaignId, "test-campaign", active);
  }

  public static CampaignEntity campaignEntity(String campaignId, String name, boolean active) {
    CampaignEntity campaign = CampaignEntity.builder()
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(DEFAULT_END_DATE)
        .active(active)
        .build();

    if (campaignId != null) {
      ReflectionTestUtils.setField(campaign, "id", campaignId);
    }
    return campaign;
  }

  public static CampaignEntity persistableCampaignEntity(String name, boolean active) {
    return CampaignEntity.builder()
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(DEFAULT_END_DATE)
        .active(active)
        .target(TargetEntity.builder()
            .os("Android")
            .country("KR")
            .minAge(20)
            .maxAge(40)
            .build())
        .creative(CreativeEntity.builder()
            .name("test-creative")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .width(300)
            .height(250)
            .build())
        .build();
  }

  public static CampaignResponseDto campaignResponse(String campaignId, boolean active) {
    return CampaignResponseDto.builder()
        .id(campaignId)
        .name("test-campaign-success")
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(DEFAULT_END_DATE)
        .active(active)
        .target(TargetResponseDto.builder()
            .id("target-1")
            .os("Android")
            .country("KR")
            .minAge(20)
            .maxAge(40)
            .createdAt(DEFAULT_TIMESTAMP)
            .updatedAt(DEFAULT_TIMESTAMP)
            .build())
        .creative(CreativeResponseDto.builder()
            .id("creative-1")
            .name("test-creative-success")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .width(300)
            .height(250)
            .createdAt(DEFAULT_TIMESTAMP)
            .updatedAt(DEFAULT_TIMESTAMP)
            .build())
        .createdAt(DEFAULT_TIMESTAMP)
        .updatedAt(DEFAULT_TIMESTAMP)
        .build();
  }

  public static CampaignCreateRequestDto campaignRequest(String name) {
    return CampaignCreateRequestDto.builder()
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(LocalDateTime.of(2026, 3, 24, 0, 0, 0))
        .target(TargetCreateRequestDto.builder().build())
        .creative(CreativeCreateRequestDto.builder()
            .name("test-creative")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .build())
        .build();
  }

  public static CampaignRedisEntity campaignRedisEntity(String campaignId) {
    return CampaignRedisEntity.builder()
        .id(campaignId)
        .name("test-campaign")
        .targetCpmMicro(10_500_000L)
        .budgetMicro(100_000_000L)
        .remainingBudgetMicro(100_000_000L)
        .startDate("20260323000000")
        .endDate("20260324235959")
        .target(TargetRedisEntity.builder()
            .os("Android")
            .country("KR")
            .minAge(20)
            .maxAge(40)
            .build())
        .creative(CreativeRedisEntity.builder()
            .id("creative-1")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .width(300)
            .height(250)
            .build())
        .build();
  }
}
