package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.KafkaBiddingLog;
import com.example.log_consumer.model.BiddingLog;
import com.example.log_consumer.repository.BiddingLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BiddingLogConsumerTest {

  @Mock
  private BiddingLogRepository biddingLogRepository;

  @InjectMocks
  private BiddingLogConsumer biddingLogConsumer;

  @Test
  void mapsMessageToEntityAndSaves() {
    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setAuctionId("auction-1")
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setPriceMicro(1200L)
        .setReceivedAt(1_714_000_000_000L)
        .build();

    biddingLogConsumer.consume(message);

    ArgumentCaptor<BiddingLog> captor = ArgumentCaptor.forClass(BiddingLog.class);
    verify(biddingLogRepository).save(captor.capture());

    BiddingLog saved = captor.getValue();
    assertThat(saved.getAuctionId()).isEqualTo("auction-1");
    assertThat(saved.getRequestId()).isEqualTo("request-1");
    assertThat(saved.getCampaignId()).isEqualTo("campaign-1");
    assertThat(saved.getCreativeId()).isEqualTo("creative-1");
    assertThat(saved.getPriceMicro()).isEqualTo(1200L);
    assertThat(saved.getReceivedAt()).isEqualTo(1_714_000_000_000L);
  }
}
