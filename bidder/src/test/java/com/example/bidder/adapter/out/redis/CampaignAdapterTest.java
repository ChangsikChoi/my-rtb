package com.example.bidder.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.model.Creative;
import com.example.bidder.domain.model.Gender;
import com.example.bidder.domain.model.Target;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest
class CampaignAdapterTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Autowired
  ReactiveStringRedisTemplate redisTemplate;
  @Autowired
  CampaignAdapter campaignAdapter;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll()
        .block();
  }

  @Test
  @DisplayName("캠페인 데이터가 레디스에서 정상 조회된다.")
  void loadCampaign_success() {
    // 캠페인 데이터 등록
    String campaignId = "camp_1";
    redisTemplate.opsForSet().add(RedisKeys.CAMPAIGN_LIST_KEY, campaignId).block();

    HashMap<String, String> campaignData = getCampaignSampleData(campaignId);

    redisTemplate.<String, String>opsForHash()
        .putAll(RedisKeys.campaignKey(campaignId), campaignData).block();

    // 레디스 캠페인 조회
    Flux<Campaign> campaignFlux = campaignAdapter.loadCampaign();

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    LocalDateTime startDate = LocalDateTime.parse("20251101000000", dateTimeFormatter);
    LocalDateTime endDate = LocalDateTime.parse("20251111000000", dateTimeFormatter);
    StepVerifier.create(campaignFlux)
        .assertNext(c -> {
              // 캠페인 데이터 검증
              assertThat(c.id()).isEqualTo(campaignId);
              assertThat(c.name()).isEqualTo("Test Campaign");
              assertThat(c.startDate()).isEqualTo(startDate);
              assertThat(c.endDate()).isEqualTo(endDate);
              assertThat(c.targetCpmMicro()).isEqualTo(1500000L);
              assertThat(c.budgetMicro()).isEqualTo(10000000L);
              assertThat(c.remainingBudgetMicro()).isEqualTo(10000000L);
              // 타겟 데이터 검증
              assertThat(c.target()).isNotNull();
              Target target = c.target();
              assertThat(target.os()).isEqualTo("Android");
              assertThat(target.country()).isEqualTo("KR");
              assertThat(target.gender()).isEqualTo(Gender.MALE);
              assertThat(target.minAge()).isEqualTo(25);
              assertThat(target.maxAge()).isEqualTo(34);
              // 소재 데이터 검증
              assertThat(c.creative()).isNotNull();
              Creative creative = c.creative();
              assertThat(creative.id()).isEqualTo("creative_1");
              assertThat(creative.imageUrl()).isEqualTo("http://example.com/image.jpg");
              assertThat(creative.clickUrl()).isEqualTo("http://example.com/click");
              assertThat(creative.width()).isEqualTo(Integer.valueOf(300));
              assertThat(creative.height()).isEqualTo(Integer.valueOf(250));
            }
        )
        .verifyComplete();
  }

  private static @NotNull HashMap<String, String> getCampaignSampleData(String campaignId) {
    HashMap<String, String> campaignData = new HashMap<>();
    campaignData.put("id", campaignId);
    campaignData.put("name", "Test Campaign");
    campaignData.put("startDate", "20251101000000");
    campaignData.put("endDate", "20251111000000");
    campaignData.put("targetCpmMicro", "1500000");
    campaignData.put("budgetMicro", "10000000");
    campaignData.put("remainingBudgetMicro", "10000000");
    campaignData.put("target.os", "Android");
    campaignData.put("target.country", "KR");
    campaignData.put("target.gender", "M");
    campaignData.put("target.minAge", "25");
    campaignData.put("target.maxAge", "34");
    campaignData.put("creative.id", "creative_1");
    campaignData.put("creative.imageUrl", "http://example.com/image.jpg");
    campaignData.put("creative.clickUrl", "http://example.com/click");
    campaignData.put("creative.width", "300");
    campaignData.put("creative.height", "250");
    return campaignData;
  }

  @Test
  @DisplayName("레디스에 캠페인 데이터가 없으면 빈 Flux가 반환된다.")
  void loadCampaign_noData_emptyFlux() {
    StepVerifier.create(campaignAdapter.loadCampaign())
        .expectNextCount(0)
        .verifyComplete();
  }
}