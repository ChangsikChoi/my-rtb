package com.example.log_consumer.consumer;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.KafkaBiddingLog;
import com.example.log_consumer.model.BiddingLog;
import com.example.log_consumer.repository.BiddingLogRepository;
import com.example.log_consumer.support.LogConsumerKafkaIntegrationTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.apache.avro.specific.SpecificRecordBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class BiddingLogKafkaRetryDltTest extends LogConsumerKafkaIntegrationTestSupport {

  private static final Duration RETRY_DLT_TIMEOUT = Duration.ofSeconds(30);

  @Autowired
  private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @MockitoBean
  private BiddingLogRepository biddingLogRepository;

  @MockitoSpyBean
  private DltLogHandler dltLogHandler;

  @Test
  void whenRepositorySaveFails_thenRetriesAndDelegatesMessageToDltHandler() {
    String auctionId = uniqueAuctionId();
    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setAuctionId(auctionId)
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setPriceMicro(1200L)
        .setReceivedAt(1_714_000_000_000L)
        .build();
    when(biddingLogRepository.save(any(BiddingLog.class)))
        .thenThrow(new DataAccessResourceFailureException("forced save failure"));

    kafkaTemplate.send("bidding-log", auctionId, message).join();
    kafkaTemplate.flush();

    await().atMost(RETRY_DLT_TIMEOUT)
        .untilAsserted(() -> {
          verify(biddingLogRepository, atLeast(3)).save(any(BiddingLog.class));
          verify(dltLogHandler).handle(
              argThat(record -> record instanceof KafkaBiddingLog biddingLog
                  && auctionId.equals(biddingLog.getAuctionId())),
              argThat(headers ->
                  "bidding-log-dlt".equals(headers.get(KafkaHeaders.RECEIVED_TOPIC))
                      && auctionId.equals(headers.get(KafkaHeaders.RECEIVED_KEY))
                      && headers.containsKey(KafkaHeaders.RECEIVED_PARTITION)
                      && headers.containsKey(KafkaHeaders.OFFSET)
                      && headers.containsKey(KafkaHeaders.ORIGINAL_TOPIC)
                      && headers.containsKey(KafkaHeaders.ORIGINAL_PARTITION)
                      && headers.containsKey(KafkaHeaders.ORIGINAL_OFFSET)
                      && headers.containsKey(KafkaHeaders.EXCEPTION_FQCN)
                      && headers.containsKey(KafkaHeaders.EXCEPTION_MESSAGE)
              )
          );
        });
  }

  private String uniqueAuctionId() {
    return "bidding-retry-dlt-" + UUID.randomUUID();
  }
}
