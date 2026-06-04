package com.example.log_consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaDeserializationFailureLogger {

  private static final Logger logger = LoggerFactory.getLogger(KafkaDeserializationFailureLogger.class);

  public void log(ConsumerRecord<?, ?> record, Exception exception) {
    logger.warn(
        "Kafka deserialization failure skipped. topic={}, partition={}, offset={}, key={}, exception={}, message={}",
        record.topic(),
        record.partition(),
        record.offset(),
        record.key(),
        exception.getClass().getName(),
        exception.getMessage()
    );
  }
}
