package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.port.in.ImpressionCommand;
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
  private SendImpressionPort sendImpressionPort;

  private final Scheduler kafkaScheduler = Schedulers.immediate();

  private ImpressionService impressionService;

  @BeforeEach
  void setUp() {
    impressionService = new ImpressionService(sendImpressionPort, kafkaScheduler);
  }

  @Test
  void whenRequestIdIsEmpty_thenReturnEmptyMono() {
    ImpressionCommand command = mock(ImpressionCommand.class);

    Mono<Impression> impressionMono = impressionService.handleImpression(command);

    StepVerifier.create(impressionMono)
        .expectNextCount(0)
        .verifyComplete();

    verify(sendImpressionPort, times(0)).sendImpression(any());
  }

  @Test
  void whenRequestIdIsNotEmpty_thenReturnImpressionMono() {
    ImpressionCommand command = new ImpressionCommand("rid1", "cid1", "crid1");

    Mono<Impression> impressionMono = impressionService.handleImpression(command);

    StepVerifier.create(impressionMono)
        .assertNext(impression -> {
          assertThat(impression.id()).isEqualTo(command.requestId());
          assertThat(impression.campaignId()).isEqualTo(command.campaignId());
          assertThat(impression.creativeId()).isEqualTo(command.creativeId());
        })
        .verifyComplete();

    verify(sendImpressionPort, times(1)).sendImpression(any());
  }
}