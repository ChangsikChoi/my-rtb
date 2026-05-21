package com.example.log_consumer.consumer;

import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class DltLogHandler {

  private static final String DLT_SUFFIX = "-dlt";
  private static final Logger biddingLogLogger = LoggerFactory.getLogger("dlt.bidding-log");
  private static final Logger impressionLogLogger = LoggerFactory.getLogger("dlt.impression-log");
  private static final Logger clickLogLogger = LoggerFactory.getLogger("dlt.click-log");
  private static final Logger winLogLogger = LoggerFactory.getLogger("dlt.win-log");
  private static final Logger unknownDltLogger = LoggerFactory.getLogger(DltLogHandler.class);

  public void handle(SpecificRecordBase message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log(loggerFor(topic), message, topic);
  }

  private Logger loggerFor(String topic) {
    return switch (sourceTopic(topic)) {
      case "bidding-log" -> biddingLogLogger;
      case "impression-log" -> impressionLogLogger;
      case "click-log" -> clickLogLogger;
      case "win-log" -> winLogLogger;
      default -> unknownDltLogger;
    };
  }

  private String sourceTopic(String topic) {
    if (topic != null && topic.endsWith(DLT_SUFFIX)) {
      return topic.substring(0, topic.length() - DLT_SUFFIX.length());
    }
    return topic;
  }

  private void log(Logger logger, SpecificRecordBase message, String topic) {
    logger.warn("DLT topic={}, message={}", topic, message);
  }
}
