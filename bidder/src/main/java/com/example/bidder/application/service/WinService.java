package com.example.bidder.application.service;

import com.example.bidder.domain.model.Win;
import com.example.bidder.domain.port.in.WinCommand;
import com.example.bidder.domain.port.in.WinUseCase;
import com.example.bidder.domain.port.out.BudgetConfirmPort;
import com.example.bidder.domain.port.out.SendWinResultPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
public class WinService implements WinUseCase {

  private final BudgetConfirmPort budgetConfirmPort;
  private final SendWinResultPort sendWinResultPort;
  private final Scheduler kafkaScheduler;

  @Override
  public Mono<Win> handleWin(WinCommand command) {

    return budgetConfirmPort.confirmBudget(command.requestId(), command.campaignId())
        .filter(success -> success)
        .map(success -> new Win(command.requestId(), command.campaignId(), command.creativeId()))
        .doOnNext(win -> Mono.fromRunnable(() -> sendWinResultPort.sendWinResult(win))
            .subscribeOn(kafkaScheduler)
            .subscribe());
  }

}
