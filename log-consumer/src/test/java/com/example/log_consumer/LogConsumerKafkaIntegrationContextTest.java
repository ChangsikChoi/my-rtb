package com.example.log_consumer;

import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LogConsumerKafkaIntegrationContextTest extends LogConsumerKafkaIntegrationTestSupport {

  @Test
  void contextLoadsWithKafkaListenersStarted() {
  }
}
