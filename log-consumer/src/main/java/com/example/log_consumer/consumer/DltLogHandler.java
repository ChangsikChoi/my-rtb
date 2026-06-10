package com.example.log_consumer.consumer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

@Component
public class DltLogHandler {

  private static final String DLT_SUFFIX = "-dlt";
  private static final Logger biddingLogLogger = LoggerFactory.getLogger("dlt.bidding-log");
  private static final Logger impressionLogLogger = LoggerFactory.getLogger("dlt.impression-log");
  private static final Logger clickLogLogger = LoggerFactory.getLogger("dlt.click-log");
  private static final Logger winLogLogger = LoggerFactory.getLogger("dlt.win-log");
  private static final Logger unknownDltLogger = LoggerFactory.getLogger(DltLogHandler.class);

  public void handle(SpecificRecordBase message, @Headers Map<String, Object> headers) {
    String topic = headerAsText(headers.get(KafkaHeaders.RECEIVED_TOPIC));
    log(loggerFor(topic), message, headers);
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

  private void log(Logger logger, SpecificRecordBase message, Map<String, Object> headers) {
    logger.warn(
        "DLT topic={}, key={}, partition={}, offset={}, originalTopic={}, originalPartition={}, originalOffset={}, exception={}, exceptionMessage={}, auctionId={}, requestId={}, message={}",
        headerAsText(headers.get(KafkaHeaders.RECEIVED_TOPIC)),
        headerAsText(headers.get(KafkaHeaders.RECEIVED_KEY)),
        headerAsLong(headers.get(KafkaHeaders.RECEIVED_PARTITION)),
        headerAsLong(headers.get(KafkaHeaders.OFFSET)),
        headerAsText(firstHeader(headers, KafkaHeaders.DLT_ORIGINAL_TOPIC, KafkaHeaders.ORIGINAL_TOPIC)),
        headerAsLong(firstHeader(headers, KafkaHeaders.DLT_ORIGINAL_PARTITION, KafkaHeaders.ORIGINAL_PARTITION)),
        headerAsLong(firstHeader(headers, KafkaHeaders.DLT_ORIGINAL_OFFSET, KafkaHeaders.ORIGINAL_OFFSET)),
        headerAsText(firstHeader(headers, KafkaHeaders.DLT_EXCEPTION_FQCN, KafkaHeaders.EXCEPTION_FQCN)),
        headerAsText(firstHeader(headers, KafkaHeaders.DLT_EXCEPTION_MESSAGE, KafkaHeaders.EXCEPTION_MESSAGE)),
        recordFieldAsText(message, "auctionId"),
        recordFieldAsText(message, "requestId"),
        message
    );
  }

  private Object firstHeader(Map<String, Object> headers, String... names) {
    for (String name : names) {
      Object value = headers.get(name);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String recordFieldAsText(SpecificRecordBase message, String fieldName) {
    Schema.Field field = message.getSchema().getField(fieldName);
    if (field == null) {
      return null;
    }
    return headerAsText(message.get(field.pos()));
  }

  private String headerAsText(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    return String.valueOf(value);
  }

  private Long headerAsLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof byte[] bytes) {
      if (bytes.length == Long.BYTES) {
        return ByteBuffer.wrap(bytes).getLong();
      }
      if (bytes.length == Integer.BYTES) {
        return (long) ByteBuffer.wrap(bytes).getInt();
      }
    }
    return null;
  }
}
