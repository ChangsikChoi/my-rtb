package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

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
    Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      dltLogHandler.handle(mock(SpecificRecordBase.class), topic);

      assertThat(appender.list).hasSize(1);
      assertThat(appender.list.get(0).getLoggerName()).isEqualTo(loggerName);
      assertThat(appender.list.get(0).getFormattedMessage()).contains("DLT topic=" + topic);
    } finally {
      logger.detachAppender(appender);
    }
  }

}
