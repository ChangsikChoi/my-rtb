package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.model.AuctionTracking;
import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.port.in.ImpressionCommand;
import com.example.bidder.domain.port.out.LoadAuctionTrackingPort;
import com.example.bidder.domain.port.out.SendImpressionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ImpressionServiceTest {

  @Mock
  private LoadAuctionTrackingPort loadAuctionTrackingPort;
  @Mock
  private SendImpressionPort sendImpressionPort;

  private final Scheduler kafkaScheduler = Schedulers.immediate();

  private ImpressionService impressionService;

  @BeforeEach
  void setUp() {
    impressionService = new ImpressionService(loadAuctionTrackingPort, sendImpressionPort, kafkaScheduler);
  }

  @Test
  void whenAuctionIdIsEmpty_thenReturnEmptyMono() {
    ImpressionCommand command = mock(ImpressionCommand.class);

    Mono<Impression> impressionMono = impressionService.handleImpression(command);

    StepVerifier.create(impressionMono)
        .expectNextCount(0)
        .verifyComplete();

    verify(sendImpressionPort, times(0)).sendImpression(any());
  }

  @Test
  void whenAuctionIdExistsAndTrackingIsFound_thenReturnImpressionMono() {
    ImpressionCommand command = new ImpressionCommand("aid1");
    when(loadAuctionTrackingPort.loadAuctionTracking("aid1"))
        .thenReturn(Mono.just(AuctionTracking.builder()
            .auctionId("aid1")
            .requestId("rid1")
            .campaignId("cid1")
            .creativeId("crid1")
            .priceMicro(100L)
            .receivedAt(1L)
            .build()));

    Mono<Impression> impressionMono = impressionService.handleImpression(command);

    StepVerifier.create(impressionMono)
        .assertNext(impression -> {
          assertThat(impression.auctionId()).isEqualTo("aid1");
          assertThat(impression.requestId()).isEqualTo("rid1");
          assertThat(impression.id()).isEqualTo("rid1");
          assertThat(impression.campaignId()).isEqualTo("cid1");
          assertThat(impression.creativeId()).isEqualTo("crid1");
        })
        .verifyComplete();

    verify(sendImpressionPort, times(1)).sendImpression(any());
  }

  @Test
  void whenTrackingDoesNotExist_thenReturnEmptyMono() {
    ImpressionCommand command = new ImpressionCommand("aid1");
    when(loadAuctionTrackingPort.loadAuctionTracking("aid1")).thenReturn(Mono.empty());

    StepVerifier.create(impressionService.handleImpression(command))
        .verifyComplete();

    verify(sendImpressionPort, times(0)).sendImpression(any());
  }
}
