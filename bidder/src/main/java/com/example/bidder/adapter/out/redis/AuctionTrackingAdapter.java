package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.model.AuctionTracking;
import com.example.bidder.domain.port.out.LoadAuctionTrackingPort;
import com.example.bidder.domain.port.out.StoreAuctionTrackingPort;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuctionTrackingAdapter implements StoreAuctionTrackingPort, LoadAuctionTrackingPort {

  private static final Duration TRACKING_TTL = Duration.ofHours(1);

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Mono<Void> storeAuctionTracking(AuctionTracking auctionTracking) {
    String trackingKey = RedisKeys.auctionTrackingKey(auctionTracking.auctionId());
    Map<String, String> values = Map.of(
        "requestId", auctionTracking.requestId(),
        "campaignId", auctionTracking.campaignId(),
        "creativeId", auctionTracking.creativeId(),
        "priceMicro", String.valueOf(auctionTracking.priceMicro()),
        "receivedAt", String.valueOf(auctionTracking.receivedAt())
    );

    return redisTemplate.opsForHash()
        .putAll(trackingKey, values)
        .then(redisTemplate.expire(trackingKey, TRACKING_TTL))
        .then();
  }

  @Override
  public Mono<AuctionTracking> loadAuctionTracking(String auctionId) {
    String trackingKey = RedisKeys.auctionTrackingKey(auctionId);

    return redisTemplate.opsForHash()
        .entries(trackingKey)
        .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())
        .filter(values -> !values.isEmpty())
        .map(values -> AuctionTracking.builder()
            .auctionId(auctionId)
            .requestId(values.get("requestId"))
            .campaignId(values.get("campaignId"))
            .creativeId(values.get("creativeId"))
            .priceMicro(Long.parseLong(values.get("priceMicro")))
            .receivedAt(Long.parseLong(values.get("receivedAt")))
            .build());
  }
}
