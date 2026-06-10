package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.KafkaBiddingLog;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;

class DltLogHandlerTest {

  private final DltLogHandler dltLogHandler = new DltLogHandler();

  @ParameterizedTest
  @CsvSource({
      "bidding-log-dlt,dlt.bidding-log",
      "impression-log-dlt,dlt.impression-log",
      "click-log-dlt,dlt.click-log",
      "win-log-dlt,dlt.win-log"
  })
  void routesDltTopicToConfiguredLogger(String topic, String loggerName) {
    String originalTopic = topic.substring(0, topic.length() - "-dlt".length());
    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setAuctionId("auction-1")
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setPriceMicro(1200L)
        .setReceivedAt(1_714_000_000_000L)
        .build();
    Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      dltLogHandler.handle(message, Map.of(
          KafkaHeaders.RECEIVED_TOPIC, topic,
          KafkaHeaders.RECEIVED_KEY, "auction-1",
          KafkaHeaders.RECEIVED_PARTITION, 0,
          KafkaHeaders.OFFSET, 1L,
          KafkaHeaders.ORIGINAL_TOPIC, originalTopic.getBytes(StandardCharsets.UTF_8),
          KafkaHeaders.ORIGINAL_PARTITION, new byte[] {0, 0, 0, 0},
          KafkaHeaders.ORIGINAL_OFFSET, new byte[] {0, 0, 0, 0, 0, 0, 0, 2},
          KafkaHeaders.EXCEPTION_FQCN, "org.example.TestException".getBytes(StandardCharsets.UTF_8),
          KafkaHeaders.EXCEPTION_MESSAGE, "forced failure".getBytes(StandardCharsets.UTF_8)
      ));

      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getLoggerName()).isEqualTo(loggerName);
      assertThat(appender.list.get(0).getFormattedMessage())
          .contains("DLT topic=" + topic)
          .contains("key=auction-1")
          .contains("partition=0")
          .contains("offset=1")
          .contains("originalTopic=" + originalTopic)
          .contains("originalPartition=0")
          .contains("originalOffset=2")
          .contains("exception=org.example.TestException")
          .contains("exceptionMessage=forced failure")
          .contains("auctionId=auction-1")
          .contains("requestId=request-1");
    } finally {
      logger.detachAppender(appender);
    }
  }

}
