package com.example.bidder.adapter.out.redis;

import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.model.Creative;
import com.example.bidder.domain.model.Creative.CreativeBuilder;
import com.example.bidder.domain.model.Gender;
import com.example.bidder.domain.model.Target;
import com.example.bidder.domain.model.Target.TargetBuilder;
import com.example.bidder.domain.port.out.LoadCampaignPort;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class CampaignAdapter implements LoadCampaignPort {

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Flux<Campaign> loadCampaign() {
    // 레디스 등록 활성화 상태 캠페인 목록 조회
    return redisTemplate.opsForSet()
        .members(RedisKeys.CAMPAIGN_LIST_KEY)
        // 레디스 캠페인 ID 목록으로 개별 캠페인 정보 조회
        .flatMap(id -> redisTemplate.opsForHash()
            .entries(RedisKeys.campaignKey(id))
            .collectMap(
                entry -> entry.getKey().toString(),
                entry -> entry.getValue().toString()
            )
            .map(this::mapToDomain)
        );
  }

  private Campaign mapToDomain(Map<String, String> hash) {
    TargetBuilder targetBuilder = Target.builder()
        .os(hash.get("target.os"))
        .country(hash.get("target.country"))
        .gender(Gender.fromCode(hash.get("target.gender")));

    if (hash.get("target.minAge") != null) {
      targetBuilder.minAge(Integer.parseInt(hash.get("target.minAge")));
    }
    if (hash.get("target.maxAge") != null) {
      targetBuilder.maxAge(Integer.parseInt(hash.get("target.maxAge")));
    }

    CreativeBuilder creativeBuilder = Creative.builder()
        .id(hash.get("creative.id"))
        .imageUrl(hash.get("creative.imageUrl"))
        .clickUrl(hash.get("creative.clickUrl"));

    if (hash.get("creative.width") != null) {
      creativeBuilder.width(Integer.parseInt(hash.get("creative.width")));
    }
    if (hash.get("creative.height") != null) {
      creativeBuilder.height(Integer.parseInt(hash.get("creative.height")));
    }

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    return Campaign.builder()
        .id(hash.get("id"))
        .name(hash.get("name"))
        .startDate(LocalDateTime.parse(hash.get("startDate"), dateTimeFormatter))
        .endDate(LocalDateTime.parse(hash.get("endDate"), dateTimeFormatter))
        .targetCpmMicro(Long.parseLong(hash.get("targetCpmMicro")))
        .budgetMicro(Long.parseLong(hash.get("budgetMicro")))
        .remainingBudgetMicro(Long.parseLong(hash.get("remainingBudgetMicro")))
        .target(targetBuilder.build())
        .creative(creativeBuilder.build())
        .build();
  }
}
