package com.example.log_consumer.consumer;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.KafkaClickLog;
import com.example.log_consumer.model.ClickLog;
import com.example.log_consumer.repository.ClickLogRepository;
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
class ClickLogKafkaRetryDltTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration RETRY_DLT_TIMEOUT = Duration.ofSeconds(30);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @MockitoBean
  private ClickLogRepository clickLogRepository;

  @MockitoSpyBean
  private DltLogHandler dltLogHandler;

  @Test
  void whenRepositorySaveFails_thenRetriesAndDelegatesMessageToDltHandler() {
    String auctionId = uniqueAuctionId();
    KafkaClickLog message = KafkaClickLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setReceivedAt(1_714_000_000_000L)
        .build();
    when(clickLogRepository.save(any(ClickLog.class)))
        .thenThrow(new DataAccessResourceFailureException("forced save failure"));

    kafkaTemplate.send("click-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(RETRY_DLT_TIMEOUT)
        .untilAsserted(() -> {
          verify(clickLogRepository, atLeast(3)).save(any(ClickLog.class));
          verify(dltLogHandler).handle(
              argThat(record -> record instanceof KafkaClickLog clickLog
                  && auctionId.equals(clickLog.getAuctionId())),
              eq("click-log-dlt")
          );
        });
  }

  private String uniqueAuctionId() {
    return "click-retry-dlt-" + UUID.randomUUID();
  }
}
