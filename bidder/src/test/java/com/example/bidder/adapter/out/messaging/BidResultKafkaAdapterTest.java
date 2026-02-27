package com.example.bidder.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KafkaBiddingLog;
import com.example.bidder.config.KafkaProducerConfig;
import com.example.bidder.domain.model.Bid;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema.Parser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(classes = {BidResultKafkaAdapter.class, KafkaProducerConfig.class})
class BidResultKafkaAdapterTest {

  private static final String CONFLUENT_VERSION = "7.4.10";
  static Network network = Network.newNetwork();
  private static final KafkaContainer kafkaContainer =
      new KafkaContainer(
          DockerImageName.parse("confluentinc/cp-kafka:" + CONFLUENT_VERSION)
              .asCompatibleSubstituteFor("apache/kafka"))
          .withNetwork(network)
          .withNetworkAliases("kafka");


  // 💡 스키마 레지스트리 컨테이너 설정
  private static final GenericContainer<?> schemaRegistryContainer = new GenericContainer<>(
      DockerImageName.parse("confluentinc/cp-schema-registry:" + CONFLUENT_VERSION))
      .withNetwork(network)
      .withNetworkAliases("schema-registry")
      .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
      .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
      .withExposedPorts(8081)
      .dependsOn(kafkaContainer);

  @Autowired
  private BidResultKafkaAdapter kafkaAdapter;
  private KafkaConsumer<String, KafkaBiddingLog> testConsumer;

  @DynamicPropertySource
  static void setApplicationProperties(DynamicPropertyRegistry registry) {
    // 1. Kafka Broker 주소 주입
    // Spring Kafka 설정 키 (application.yml/properties에 맞춰 사용)
    registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);

    // 2. Schema Registry 주소 주입
    // KafkaProducerConfig에서 사용한 설정 키에 맞춰 주입
    registry.add("spring.kafka.schema-registry.url", () ->
        String.format("http://%s:%d", schemaRegistryContainer.getHost(),
            schemaRegistryContainer.getMappedPort(8081)));
  }

  // 카프카와 스키마 레지스트리 접속 URL 획득
  private static String getKafkaBootstrapServers() {
    return kafkaContainer.getBootstrapServers();
  }

  private static String getSchemaRegistryUrl() {
    // 스키마 레지스트리의 호스트 IP와 매핑된 포트 사용
    return String.format("http://%s:%d", schemaRegistryContainer.getHost(),
        schemaRegistryContainer.getMappedPort(8081));
  }

  // 테스트 시작 전 컨테이너 실행
  @BeforeAll
  static void setUp() {
    kafkaContainer.start();
    schemaRegistryContainer.start();
    String biddingLogScheme = ""
        + "{"
        + "  \"type\": \"record\","
        + "  \"name\": \"KafkaBiddingLog\","
        + "  \"namespace\": \"com.example\","
        + "  \"fields\": ["
        + "    {\"name\": \"requestId\", \"type\": \"string\"},"
        + "    {\"name\": \"campaignId\", \"type\": \"string\"},"
        + "    {\"name\": \"creativeId\", \"type\": \"string\"},"
        + "    {\"name\": \"price\", \"type\": \"double\"}"
        + "  ]"
        + "}";
    try (CachedSchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient(
        getSchemaRegistryUrl(), 10)) {

      AvroSchema avroSchema = new AvroSchema(new Parser().parse(biddingLogScheme));
      schemaRegistryClient.register("bidding-log", avroSchema);
    } catch (IOException | RestClientException e) {
      throw new RuntimeException(e);
    }
  }

  // 테스트 종료 후 컨테이너 종료
  @AfterAll
  static void tearDown() {
    schemaRegistryContainer.stop();
    kafkaContainer.stop();
    network.close();
  }

  @BeforeEach
  void setUpEach() {

    // Avro 메시지를 수신할 수 있는 테스트용 컨슈머 설정
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
    props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, getSchemaRegistryUrl());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class);
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    testConsumer = new KafkaConsumer<>(props);
    String TOPIC = "bidding-log";
    testConsumer.subscribe(Collections.singletonList(TOPIC));
  }

  @Test
  @DisplayName("Adapter가 Avro 스키마를 포함한 메시지를 Kafka에 성공적으로 발행한다")
  void shouldProduceMessageWithSchema() {
    // given: Avro로 생성된 도메인 객체
    Bid bid = Bid.builder()
        .bidPriceCpmMicro(1_000_000_000L)
        .campaignId("c123")
        .creativeId("cr456")
        .requestId("req789")
        .build();

    // when: 테스트 대상 어댑터의 send 메서드 호출
    kafkaAdapter.sendBidResult(bid); // (send 메서드가 Avro 객체를 받는다고 가정)

    // then: 테스트 컨슈머가 메시지를 수신했는지 검증
    ConsumerRecords<String, KafkaBiddingLog> records = testConsumer.poll(Duration.ofSeconds(10));

    assertThat(records.isEmpty()).isFalse();

    ConsumerRecord<String, KafkaBiddingLog> receivedRecord = records.iterator().next();
    assertThat(receivedRecord.value().getCampaignId()).isEqualTo("c123");
    assertThat(receivedRecord.value().getPriceMicro()).isEqualTo(1_000_000L);
  }
}