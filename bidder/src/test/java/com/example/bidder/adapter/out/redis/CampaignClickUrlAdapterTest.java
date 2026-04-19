package com.example.bidder.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest
class CampaignClickUrlAdapterTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Autowired
  private ReactiveStringRedisTemplate redisTemplate;
  @Autowired
  private CampaignClickUrlAdapter campaignClickUrlAdapter;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
  }

  @Test
  @DisplayName("creative id가 일치하면 click url을 반환한다")
  void loadClickUrl_whenCreativeMatches_thenReturnClickUrl() {
    redisTemplate.<String, String>opsForHash().putAll(
        RedisKeys.campaignKey("campaign_1"),
        Map.of(
            "creative.id", "creative_1",
            "creative.clickUrl", "http://example.com/click"
        )
    ).block();

    StepVerifier.create(campaignClickUrlAdapter.loadClickUrl("campaign_1", "creative_1"))
        .assertNext(clickUrl -> assertThat(clickUrl).isEqualTo("http://example.com/click"))
        .verifyComplete();
  }

  @Test
  @DisplayName("creative id가 다르면 빈 Mono를 반환한다")
  void loadClickUrl_whenCreativeDoesNotMatch_thenReturnEmptyMono() {
    redisTemplate.<String, String>opsForHash().putAll(
        RedisKeys.campaignKey("campaign_1"),
        Map.of(
            "creative.id", "creative_1",
            "creative.clickUrl", "http://example.com/click"
        )
    ).block();

    StepVerifier.create(campaignClickUrlAdapter.loadClickUrl("campaign_1", "creative_2"))
        .verifyComplete();
  }
}
