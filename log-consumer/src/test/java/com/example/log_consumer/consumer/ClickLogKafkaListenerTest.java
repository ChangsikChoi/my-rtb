package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.KafkaClickLog;
import com.example.log_consumer.model.ClickLog;
import com.example.log_consumer.repository.ClickLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class ClickLogKafkaListenerTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(20);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @Autowired
  private ClickLogRepository clickLogRepository;

  @Test
  void whenValidAvroMessageIsPublished_thenLogIsSaved() {
    String auctionId = uniqueAuctionId();
    KafkaClickLog message = KafkaClickLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-4")
        .setCampaignId("campaign-4")
        .setCreativeId("creative-4")
        .setReceivedAt(1_714_000_000_003L)
        .build();

    kafkaTemplate.send("click-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(LISTENER_TIMEOUT)
        .untilAsserted(() -> assertThat(clickLogRepository.findByAuctionId(auctionId)).isPresent());

    ClickLog saved = clickLogRepository.findByAuctionId(auctionId).orElseThrow();
    assertThat(saved.getAuctionId()).isEqualTo(auctionId);
    assertThat(saved.getRequestId()).isEqualTo("request-4");
    assertThat(saved.getCampaignId()).isEqualTo("campaign-4");
    assertThat(saved.getCreativeId()).isEqualTo("creative-4");
    assertThat(saved.getReceivedAt()).isEqualTo(1_714_000_000_003L);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  private String uniqueAuctionId() {
    return "click-" + UUID.randomUUID();
  }
}
