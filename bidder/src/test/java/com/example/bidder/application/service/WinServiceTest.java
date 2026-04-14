package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.model.AuctionTracking;
import com.example.bidder.domain.model.Win;
import com.example.bidder.domain.port.in.WinCommand;
import com.example.bidder.domain.port.out.BudgetConfirmPort;
import com.example.bidder.domain.port.out.LoadAuctionTrackingPort;
import com.example.bidder.domain.port.out.SendWinResultPort;
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
class WinServiceTest {

  @Mock
  private BudgetConfirmPort budgetConfirmPort;
  @Mock
  private LoadAuctionTrackingPort loadAuctionTrackingPort;
  @Mock
  private SendWinResultPort sendWinResultPort;

  private final Scheduler kafkaScheduler = Schedulers.immediate();

  private WinService winService;

  @BeforeEach
  void setUp() {
    winService = new WinService(
        budgetConfirmPort,
        loadAuctionTrackingPort,
        sendWinResultPort,
        kafkaScheduler
    );
  }

  @Test
  void whenConfirmBudgetSuccess_thenReturnWinAndSendResult() {
    WinCommand command = new WinCommand("aid1");

    when(loadAuctionTrackingPort.loadAuctionTracking("aid1"))
        .thenReturn(Mono.just(AuctionTracking.builder()
            .auctionId("aid1")
            .requestId("rid1")
            .campaignId("cid1")
            .creativeId("crid1")
            .priceMicro(100L)
            .receivedAt(1L)
            .build()));
    when(budgetConfirmPort.confirmBudget(any(), any())).thenReturn(Mono.just(true));
    doNothing().when(sendWinResultPort).sendWinResult(any());

    Mono<Win> winResult = winService.handleWin(command);

    StepVerifier.create(winResult)
        .assertNext(win -> {
          assertThat(win.auctionId()).isEqualTo("aid1");
          assertThat(win.requestId()).isEqualTo("rid1");
          assertThat(win.id()).isEqualTo("rid1");
          assertThat(win.campaignId()).isEqualTo("cid1");
          assertThat(win.creativeId()).isEqualTo("crid1");
        })
        .verifyComplete();

    verify(sendWinResultPort, times(1)).sendWinResult(any());
  }

  @Test
  void whenConfirmBudgetFailed_thenReturnEmptyMono() {
    WinCommand command = new WinCommand("aid1");

    when(loadAuctionTrackingPort.loadAuctionTracking("aid1"))
        .thenReturn(Mono.just(AuctionTracking.builder()
            .auctionId("aid1")
            .requestId("rid1")
            .campaignId("cid1")
            .creativeId("crid1")
            .priceMicro(100L)
            .receivedAt(1L)
            .build()));
    when(budgetConfirmPort.confirmBudget(any(), any())).thenReturn(Mono.just(false));

    Mono<Win> winResult = winService.handleWin(command);

    StepVerifier.create(winResult)
        .expectNextCount(0)
        .verifyComplete();

    verify(sendWinResultPort, times(0)).sendWinResult(any());
  }

  @Test
  void whenTrackingDoesNotExist_thenReturnEmptyMono() {
    WinCommand command = new WinCommand("aid1");

    when(loadAuctionTrackingPort.loadAuctionTracking("aid1")).thenReturn(Mono.empty());

    StepVerifier.create(winService.handleWin(command))
        .verifyComplete();

    verify(sendWinResultPort, times(0)).sendWinResult(any());
  }

}
