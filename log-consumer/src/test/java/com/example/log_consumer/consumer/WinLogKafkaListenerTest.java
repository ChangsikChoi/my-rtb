package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.KafkaWinLog;
import com.example.log_consumer.model.WinLog;
import com.example.log_consumer.repository.WinLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class WinLogKafkaListenerTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(20);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @Autowired
  private WinLogRepository winLogRepository;

  @Test
  void whenValidAvroMessageIsPublished_thenLogIsSaved() {
    String auctionId = uniqueAuctionId();
    KafkaWinLog message = KafkaWinLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-2")
        .setCampaignId("campaign-2")
        .setCreativeId("creative-2")
        .setReceivedAt(1_714_000_000_001L)
        .build();

    kafkaTemplate.send("win-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(LISTENER_TIMEOUT)
        .untilAsserted(() -> assertThat(winLogRepository.findByAuctionId(auctionId)).isPresent());

    WinLog saved = winLogRepository.findByAuctionId(auctionId).orElseThrow();
    assertThat(saved.getAuctionId()).isEqualTo(auctionId);
    assertThat(saved.getRequestId()).isEqualTo("request-2");
    assertThat(saved.getCampaignId()).isEqualTo("campaign-2");
    assertThat(saved.getCreativeId()).isEqualTo("creative-2");
    assertThat(saved.getReceivedAt()).isEqualTo(1_714_000_000_001L);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  private String uniqueAuctionId() {
    return "win-" + UUID.randomUUID();
  }
}
