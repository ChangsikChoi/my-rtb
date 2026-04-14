package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.out.LoadAuctionTrackingPort;
import com.example.bidder.domain.port.out.LoadClickUrlPort;
import com.example.bidder.domain.port.out.SendClickPort;
import com.example.bidder.domain.model.AuctionTracking;
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
class ClickServiceTest {

  private static final long RECEIVED_AT = 1_712_966_400_000L;

  @Mock
  private LoadAuctionTrackingPort loadAuctionTrackingPort;
  @Mock
  private LoadClickUrlPort loadClickUrlPort;
  @Mock
  private SendClickPort sendClickPort;

  private final Scheduler kafkaScheduler = Schedulers.immediate();

  private ClickService clickService;

  @BeforeEach
  void setUp() {
    clickService = new ClickService(
        loadAuctionTrackingPort,
        loadClickUrlPort,
        sendClickPort,
        kafkaScheduler
    );
  }

  @Test
  void whenAuctionIdIsEmpty_thenReturnEmptyMono() {
    ClickCommand command = mock(ClickCommand.class);

    Mono<String> clickMono = clickService.handleClick(command);

    StepVerifier.create(clickMono)
        .expectNextCount(0)
        .verifyComplete();

    verify(sendClickPort, times(0)).sendClick(any());
  }

  @Test
  void whenTrackingAndClickUrlExist_thenReturnClickUrl() {
    ClickCommand command = new ClickCommand("aid1", RECEIVED_AT);
    when(loadAuctionTrackingPort.loadAuctionTracking("aid1"))
        .thenReturn(Mono.just(AuctionTracking.builder()
            .auctionId("aid1")
            .requestId("rid1")
            .campaignId("cid1")
            .creativeId("crid1")
            .priceMicro(100L)
            .receivedAt(1L)
            .build()));
    when(loadClickUrlPort.loadClickUrl("cid1", "crid1")).thenReturn(Mono.just("http://example.com"));

    Mono<String> clickMono = clickService.handleClick(command);

    StepVerifier.create(clickMono)
        .assertNext(clickUrl -> assertThat(clickUrl).isEqualTo("http://example.com"))
        .verifyComplete();

    verify(sendClickPort, times(1)).sendClick(any());
  }

  @Test
  void whenTrackingDoesNotExist_thenReturnEmptyMono() {
    ClickCommand command = new ClickCommand("aid1", RECEIVED_AT);
    when(loadAuctionTrackingPort.loadAuctionTracking("aid1")).thenReturn(Mono.empty());

    StepVerifier.create(clickService.handleClick(command))
        .verifyComplete();

    verify(sendClickPort, times(0)).sendClick(any());
  }

  @Test
  void whenClickUrlDoesNotExist_thenReturnEmptyMono() {
    ClickCommand command = new ClickCommand("aid1", RECEIVED_AT);
    when(loadAuctionTrackingPort.loadAuctionTracking("aid1"))
        .thenReturn(Mono.just(AuctionTracking.builder()
            .auctionId("aid1")
            .requestId("rid1")
            .campaignId("cid1")
            .creativeId("crid1")
            .priceMicro(100L)
            .receivedAt(1L)
            .build()));
    when(loadClickUrlPort.loadClickUrl("cid1", "crid1")).thenReturn(Mono.empty());

    StepVerifier.create(clickService.handleClick(command))
        .verifyComplete();

    verify(sendClickPort, times(1)).sendClick(any());
  }

}
