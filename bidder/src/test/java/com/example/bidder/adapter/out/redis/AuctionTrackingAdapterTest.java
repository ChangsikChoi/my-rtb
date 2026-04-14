package com.example.bidder.adapter.out.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bidder.domain.model.AuctionTracking;
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
class AuctionTrackingAdapterTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Autowired
  private ReactiveStringRedisTemplate redisTemplate;
  @Autowired
  private AuctionTrackingAdapter auctionTrackingAdapter;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
  }

  @Test
  @DisplayName("auction tracking을 저장한 뒤 조회하면 동일한 데이터가 반환된다")
  void storeAndLoadAuctionTracking() {
    AuctionTracking auctionTracking = AuctionTracking.builder()
        .auctionId("auction_1")
        .requestId("request_1")
        .campaignId("campaign_1")
        .creativeId("creative_1")
        .priceMicro(123_000L)
        .receivedAt(1_712_966_400_000L)
        .build();

    StepVerifier.create(auctionTrackingAdapter.storeAuctionTracking(auctionTracking))
        .verifyComplete();

    StepVerifier.create(auctionTrackingAdapter.loadAuctionTracking("auction_1"))
        .assertNext(loaded -> assertThat(loaded).isEqualTo(auctionTracking))
        .verifyComplete();

    StepVerifier.create(redisTemplate.getExpire(RedisKeys.auctionTrackingKey("auction_1")))
        .assertNext(ttl -> assertThat(ttl).isPositive())
        .verifyComplete();
  }
}
