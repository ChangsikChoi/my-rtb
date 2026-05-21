package com.example.log_consumer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;

class KafkaRetryTopicConfigTest {

  @Test
  void createsRetryTopicConfigurationForLogTopics() {
    KafkaRetryTopicConfig retryTopicConfig = new KafkaRetryTopicConfig(3, 1000, 2);
    RetryTopicConfiguration configuration = retryTopicConfig.logRetryTopicConfiguration(kafkaTemplate());

    assertThat(configuration.hasConfigurationForTopics(new String[] {"bidding-log"})).isTrue();
    assertThat(configuration.hasConfigurationForTopics(new String[] {"impression-log"})).isTrue();
    assertThat(configuration.hasConfigurationForTopics(new String[] {"click-log"})).isTrue();
    assertThat(configuration.hasConfigurationForTopics(new String[] {"win-log"})).isTrue();
    assertThat(configuration.hasConfigurationForTopics(new String[] {"unknown-log"})).isFalse();
    assertThat(configuration.getDltHandlerMethod().getMethodName()).isEqualTo("handle");
  }

  @SuppressWarnings("unchecked")
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate() {
    return mock(KafkaTemplate.class);
  }
}
