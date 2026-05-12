package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.KafkaBiddingLog;
import com.example.log_consumer.model.BiddingLog;
import com.example.log_consumer.repository.BiddingLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class BiddingLogKafkaListenerTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(20);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @Autowired
  private BiddingLogRepository biddingLogRepository;

  @Test
  void whenValidAvroMessageIsPublished_thenLogIsSaved() {
    String auctionId = uniqueAuctionId();
    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setPriceMicro(1200L)
        .setReceivedAt(1_714_000_000_000L)
        .build();

    kafkaTemplate.send("bidding-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(LISTENER_TIMEOUT)
        .untilAsserted(() -> assertThat(biddingLogRepository.findByAuctionId(auctionId)).isPresent());

    BiddingLog saved = biddingLogRepository.findByAuctionId(auctionId).orElseThrow();
    assertThat(saved.getAuctionId()).isEqualTo(auctionId);
    assertThat(saved.getRequestId()).isEqualTo("request-1");
    assertThat(saved.getCampaignId()).isEqualTo("campaign-1");
    assertThat(saved.getCreativeId()).isEqualTo("creative-1");
    assertThat(saved.getPriceMicro()).isEqualTo(1200L);
    assertThat(saved.getReceivedAt()).isEqualTo(1_714_000_000_000L);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  private String uniqueAuctionId() {
    return "bidding-" + UUID.randomUUID();
  }
}
