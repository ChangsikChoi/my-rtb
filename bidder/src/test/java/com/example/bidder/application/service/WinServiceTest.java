package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.model.Win;
import com.example.bidder.domain.port.in.WinCommand;
import com.example.bidder.domain.port.out.BudgetConfirmPort;
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
  private SendWinResultPort sendWinResultPort;

  private final Scheduler kafkaScheduler = Schedulers.immediate();

  private WinService winService;

  @BeforeEach
  void setUp() {
    winService = new WinService(budgetConfirmPort, sendWinResultPort, kafkaScheduler);
  }

  @Test
  void whenConfirmBudgetSuccess_thenReturnWinAndSendResult() {
    WinCommand command = new WinCommand("rid1", "cid1", "crid1");

    when(budgetConfirmPort.confirmBudget(any(), any())).thenReturn(Mono.just(true));
    doNothing().when(sendWinResultPort).sendWinResult(any());

    Mono<Win> winResult = winService.handleWin(command);

    StepVerifier.create(winResult)
        .assertNext(win -> {
          assertThat(win.id()).isEqualTo(command.requestId());
          assertThat(win.campaignId()).isEqualTo(command.campaignId());
          assertThat(win.creativeId()).isEqualTo(command.creativeId());
        })
        .verifyComplete();

    verify(sendWinResultPort, times(1)).sendWinResult(any());
  }

  @Test
  void whenConfirmBudgetFailed_thenReturnEmptyMono() {
    WinCommand command = mock(WinCommand.class);

    when(budgetConfirmPort.confirmBudget(any(), any())).thenReturn(Mono.just(false));

    Mono<Win> winResult = winService.handleWin(command);

    StepVerifier.create(winResult)
        .expectNextCount(0)
        .verifyComplete();

    verify(sendWinResultPort, times(0)).sendWinResult(any());
  }

}