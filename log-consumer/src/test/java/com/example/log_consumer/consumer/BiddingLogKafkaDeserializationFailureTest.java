package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.KafkaBiddingLog;
import com.example.log_consumer.model.BiddingLog;
import com.example.log_consumer.repository.BiddingLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class BiddingLogKafkaDeserializationFailureTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration LISTENER_TIMEOUT = Duration.ofSeconds(20);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @Autowired
  private BiddingLogRepository biddingLogRepository;

  @MockitoSpyBean
  private KafkaDeserializationFailureLogger deserializationFailureLogger;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Test
  void whenInvalidPayloadIsPublished_thenConsumerLogsSkipsDltAndContinuesWithNextValidMessage() {
    String invalidAuctionId = "bidding-deserialization-invalid-" + UUID.randomUUID();
    String validAuctionId = "bidding-deserialization-valid-" + UUID.randomUUID();

    Map<String, Object> rawProducerConfig = Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class
    );
    DefaultKafkaProducerFactory<String, byte[]> producerFactory = new DefaultKafkaProducerFactory<>(rawProducerConfig);
    try {
      KafkaTemplate<String, byte[]> rawKafkaTemplate = new KafkaTemplate<>(producerFactory);
      rawKafkaTemplate.send("bidding-log", invalidAuctionId, new byte[] {1, 2, 3, 4}).join();
      rawKafkaTemplate.flush();
    } finally {
      producerFactory.destroy();
    }

    KafkaBiddingLog validMessage = KafkaBiddingLog.newBuilder()
        .setAuctionId(validAuctionId)
        .setRequestId("request-valid")
        .setCampaignId("campaign-valid")
        .setCreativeId("creative-valid")
        .setPriceMicro(1200L)
        .setReceivedAt(1_714_000_000_000L)
        .build();
    kafkaTemplate.send("bidding-log", validAuctionId, validMessage).join();
    kafkaTemplate.flush();

    await().atMost(LISTENER_TIMEOUT)
        .untilAsserted(() -> assertThat(biddingLogRepository.findByAuctionId(validAuctionId)).isPresent());

    verify(deserializationFailureLogger).log(any(ConsumerRecord.class), any(Exception.class));
    assertThat(biddingLogRepository.findByAuctionId(invalidAuctionId)).isEmpty();

    Map<String, Object> dltConsumerConfig = Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG, "bidding-deserialization-dlt-probe-" + UUID.randomUUID(),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class
    );
    try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(dltConsumerConfig)) {
      consumer.subscribe(List.of("bidding-log-dlt"));
      ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(2));
      assertThat(records)
          .noneMatch(record -> invalidAuctionId.equals(record.key()));
    }

    BiddingLog saved = biddingLogRepository.findByAuctionId(validAuctionId).orElseThrow();
    assertThat(saved.getRequestId()).isEqualTo("request-valid");
  }
}
