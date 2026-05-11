package com.example.log_consumer.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.KafkaWinLog;
import com.example.log_consumer.model.WinLog;
import com.example.log_consumer.repository.WinLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WinLogConsumerTest {

  @Mock
  private WinLogRepository winLogRepository;

  @InjectMocks
  private WinLogConsumer winLogConsumer;

  @Test
  void mapsMessageToEntityAndSaves() {
    KafkaWinLog message = KafkaWinLog.newBuilder()
        .setAuctionId("auction-1")
        .setRequestId("request-1")
        .setCampaignId("campaign-1")
        .setCreativeId("creative-1")
        .setReceivedAt(1_714_000_000_000L)
        .build();

    winLogConsumer.consume(message);

    ArgumentCaptor<WinLog> captor = ArgumentCaptor.forClass(WinLog.class);
    verify(winLogRepository).save(captor.capture());

    WinLog saved = captor.getValue();
    assertThat(saved.getAuctionId()).isEqualTo("auction-1");
    assertThat(saved.getRequestId()).isEqualTo("request-1");
    assertThat(saved.getCampaignId()).isEqualTo("campaign-1");
    assertThat(saved.getCreativeId()).isEqualTo("creative-1");
    assertThat(saved.getReceivedAt()).isEqualTo(1_714_000_000_000L);
  }
}
