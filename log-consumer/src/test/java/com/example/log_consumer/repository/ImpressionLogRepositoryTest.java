package com.example.log_consumer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.log_consumer.model.ImpressionLog;
import com.example.log_consumer.support.LogConsumerIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ImpressionLogRepositoryTest extends LogConsumerIntegrationTestSupport {

  @Autowired
  private ImpressionLogRepository impressionLogRepository;

  @Test
  void savesAndFindsImpressionLog() {
    ImpressionLog log = ImpressionLog.builder()
        .auctionId("auction-1")
        .requestId("request-1")
        .campaignId("campaign-1")
        .creativeId("creative-1")
        .receivedAt(1_714_000_000_000L)
        .build();

    ImpressionLog saved = impressionLogRepository.saveAndFlush(log);

    ImpressionLog found = impressionLogRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getId()).isNotNull();
    assertThat(found.getAuctionId()).isEqualTo("auction-1");
    assertThat(found.getRequestId()).isEqualTo("request-1");
    assertThat(found.getCampaignId()).isEqualTo("campaign-1");
    assertThat(found.getCreativeId()).isEqualTo("creative-1");
    assertThat(found.getReceivedAt()).isEqualTo(1_714_000_000_000L);
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
