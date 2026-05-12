package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.KafkaImpressionLog;
import com.example.log_consumer.model.ImpressionLog;
import com.example.log_consumer.repository.ImpressionLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class ImpressionLogKafkaListenerTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(20);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @Autowired
  private ImpressionLogRepository impressionLogRepository;

  @Test
  void whenValidAvroMessageIsPublished_thenLogIsSaved() {
    String auctionId = uniqueAuctionId();
    KafkaImpressionLog message = KafkaImpressionLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-3")
        .setCampaignId("campaign-3")
        .setCreativeId("creative-3")
        .setReceivedAt(1_714_000_000_002L)
        .build();

    kafkaTemplate.send("impression-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(LISTENER_TIMEOUT)
        .untilAsserted(() -> assertThat(impressionLogRepository.findByAuctionId(auctionId)).isPresent());

    ImpressionLog saved = impressionLogRepository.findByAuctionId(auctionId).orElseThrow();
    assertThat(saved.getAuctionId()).isEqualTo(auctionId);
    assertThat(saved.getRequestId()).isEqualTo("request-3");
    assertThat(saved.getCampaignId()).isEqualTo("campaign-3");
    assertThat(saved.getCreativeId()).isEqualTo("creative-3");
    assertThat(saved.getReceivedAt()).isEqualTo(1_714_000_000_002L);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  private String uniqueAuctionId() {
    return "impression-" + UUID.randomUUID();
  }
}
