package com.example.log_consumer.consumer;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.KafkaImpressionLog;
import com.example.log_consumer.model.ImpressionLog;
import com.example.log_consumer.repository.ImpressionLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class ImpressionLogKafkaRetryDltTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration RETRY_DLT_TIMEOUT = Duration.ofSeconds(30);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @MockitoBean
  private ImpressionLogRepository impressionLogRepository;

  @MockitoSpyBean
  private DltLogHandler dltLogHandler;

  @Test
  void whenRepositorySaveFails_thenRetriesAndDelegatesMessageToDltHandler() {
    String auctionId = uniqueAuctionId();
    KafkaImpressionLog message = KafkaImpressionLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setReceivedAt(1_714_000_000_000L)
        .build();
    when(impressionLogRepository.save(any(ImpressionLog.class)))
        .thenThrow(new DataAccessResourceFailureException("forced save failure"));

    kafkaTemplate.send("impression-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(RETRY_DLT_TIMEOUT)
        .untilAsserted(() -> {
          verify(impressionLogRepository, atLeast(3)).save(any(ImpressionLog.class));
          verify(dltLogHandler).handle(
              argThat(record -> record instanceof KafkaImpressionLog impressionLog
                  && auctionId.equals(impressionLog.getAuctionId())),
              eq("impression-log-dlt")
          );
        });
  }

  private String uniqueAuctionId() {
    return "impression-retry-dlt-" + UUID.randomUUID();
  }
}
