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
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.request.CreativeCreateRequest;
import com.example.ad_manager.model.request.TargetCreateRequest;
import com.example.ad_manager.model.response.CampaignResponse;
import com.example.ad_manager.model.response.CreativeResponse;
import com.example.ad_manager.model.response.TargetResponse;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CreativeRedisEntity;
import com.example.ad_manager.redis.TargetRedisEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CampaignTestFixtures {

  private static final BigDecimal DEFAULT_TARGET_CPM = BigDecimal.valueOf(10.5);
  private static final BigDecimal DEFAULT_BUDGET = BigDecimal.valueOf(100.0);
  private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.of(2026, 3, 23, 0, 0, 0);
  private static final LocalDateTime DEFAULT_END_DATE = LocalDateTime.of(2026, 3, 24, 23, 59, 59);
  private static final LocalDateTime DEFAULT_TIMESTAMP = LocalDateTime.of(2026, 3, 22, 10, 0, 0);

  private CampaignTestFixtures() {
  }

  public static CampaignEntity campaignEntity(boolean active) {
    return campaignEntity("test-campaign", active);
  }

  public static CampaignEntity campaignEntity(String name, boolean active) {
    return CampaignEntity.builder()
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(DEFAULT_END_DATE)
        .active(active)
        .build();
  }

  public static CampaignEntity persistableCampaignEntity(String name, boolean active) {
    return CampaignEntity.builder()
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(DEFAULT_END_DATE)
        .active(active)
        .target(defaultTargetEntity())
        .creative(defaultCreativeEntity())
        .build();
  }

  public static CampaignResponseDto campaignResponse(String campaignId, boolean active) {
    return campaignResponse(campaignId, "test-campaign", active);
  }

  public static CampaignResponseDto campaignResponse(String campaignId, String name, boolean active) {
    return CampaignResponseDto.builder()
        .id(campaignId)
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE)
        .endDate(DEFAULT_END_DATE)
        .active(active)
        .target(defaultTargetResponseDto())
        .creative(defaultCreativeResponseDto())
        .createdAt(DEFAULT_TIMESTAMP)
        .updatedAt(DEFAULT_TIMESTAMP)
        .build();
  }

  public static CampaignResponse campaignHttpResponse(String campaignId, boolean active) {
    return campaignHttpResponse(campaignId, "test-campaign", active);
  }

  public static CampaignResponse campaignHttpResponse(String campaignId, String name,
                                                      boolean active) {
    return CampaignResponse.builder()
        .id(campaignId)
        .name(name)
        .targetCpm(DEFAULT_TARGET_CPM)
        .budget(DEFAULT_BUDGET)
        .startDate(DEFAULT_START_DATE.toLocalDate())
        .endDate(DEFAULT_END_DATE.toLocalDate())
        .active(active)
        .target(defaultTargetResponse())
        .creative(defaultCreativeResponse())
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
        .endDate(DEFAULT_END_DATE)
        .target(TargetCreateRequestDto.builder().build())
        .creative(CreativeCreateRequestDto.builder()
            .name("test-creative")
            .imageUrl("https://example.com/a.png")
            .clickUrl("https://example.com")
            .build())
        .build();
  }

  public static CampaignCreateRequest campaignCreateRequest(String name) {
    return new CampaignCreateRequest(
        name,
        DEFAULT_TARGET_CPM,
        DEFAULT_BUDGET,
        DEFAULT_START_DATE.toLocalDate(),
        DEFAULT_END_DATE.toLocalDate(),
        new TargetCreateRequest("Android", "KR", 20, 40),
        new CreativeCreateRequest(
            "test-creative-success",
            "https://example.com/a.png",
            "https://example.com",
            300,
            250
        )
    );
  }

  public static CampaignCreateRequest invalidCampaignCreateRequest() {
    return new CampaignCreateRequest(
        "",
        BigDecimal.TEN.negate(),
        BigDecimal.ZERO,
        LocalDate.of(2026, 3, 24),
        LocalDate.of(2026, 3, 23),
        new TargetCreateRequest(null, null, 30, 20),
        new CreativeCreateRequest("", "", "", -1, -1)
    );
  }

  public static CampaignRedisEntity campaignRedisEntity(String campaignId) {
    return campaignRedisEntity(campaignId, "test-campaign");
  }

  public static CampaignRedisEntity campaignRedisEntity(String campaignId, String name) {
    return CampaignRedisEntity.builder()
        .id(campaignId)
        .name(name)
        .targetCpmMicro(10_500_000L)
        .budgetMicro(100_000_000L)
        .remainingBudgetMicro(100_000_000L)
        .startDate("20260323000000")
        .endDate("20260324235959")
        .target(defaultTargetRedisEntity())
        .creative(defaultCreativeRedisEntity())
        .build();
  }

  private static TargetEntity defaultTargetEntity() {
    return TargetEntity.builder()
        .os("Android")
        .country("KR")
        .minAge(20)
        .maxAge(40)
        .build();
  }

  private static CreativeEntity defaultCreativeEntity() {
    return CreativeEntity.builder()
        .name("test-creative")
        .imageUrl("https://example.com/a.png")
        .clickUrl("https://example.com")
        .width(300)
        .height(250)
        .build();
  }

  private static TargetResponseDto defaultTargetResponseDto() {
    return TargetResponseDto.builder()
        .id("target-1")
        .os("Android")
        .country("KR")
        .minAge(20)
        .maxAge(40)
        .createdAt(DEFAULT_TIMESTAMP)
        .updatedAt(DEFAULT_TIMESTAMP)
        .build();
  }

  private static CreativeResponseDto defaultCreativeResponseDto() {
    return CreativeResponseDto.builder()
        .id("creative-1")
        .name("test-creative-success")
        .imageUrl("https://example.com/a.png")
        .clickUrl("https://example.com")
        .width(300)
        .height(250)
        .createdAt(DEFAULT_TIMESTAMP)
        .updatedAt(DEFAULT_TIMESTAMP)
        .build();
  }

  private static TargetRedisEntity defaultTargetRedisEntity() {
    return TargetRedisEntity.builder()
        .os("Android")
        .country("KR")
        .minAge(20)
        .maxAge(40)
        .build();
  }

  private static CreativeRedisEntity defaultCreativeRedisEntity() {
    return CreativeRedisEntity.builder()
        .id("creative-1")
        .imageUrl("https://example.com/a.png")
        .clickUrl("https://example.com")
        .width(300)
        .height(250)
        .build();
  }

  private static TargetResponse defaultTargetResponse() {
    return TargetResponse.builder()
        .id("target-1")
        .os("Android")
        .country("KR")
        .minAge(20)
        .maxAge(40)
        .createdAt(DEFAULT_TIMESTAMP)
        .updatedAt(DEFAULT_TIMESTAMP)
        .build();
  }

  private static CreativeResponse defaultCreativeResponse() {
    return CreativeResponse.builder()
        .id("creative-1")
        .name("test-creative-success")
        .imageUrl("https://example.com/a.png")
        .clickUrl("https://example.com")
        .width(300)
        .height(250)
        .createdAt(DEFAULT_TIMESTAMP)
        .updatedAt(DEFAULT_TIMESTAMP)
        .build();
  }
}
