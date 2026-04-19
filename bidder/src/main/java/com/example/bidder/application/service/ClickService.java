package com.example.bidder.application.service;

import com.example.bidder.domain.model.Click;
import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.in.ClickUseCase;
import com.example.bidder.domain.port.out.LoadAuctionTrackingPort;
import com.example.bidder.domain.port.out.LoadClickUrlPort;
import com.example.bidder.domain.port.out.SendClickPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
public class ClickService implements ClickUseCase {

  private final LoadAuctionTrackingPort loadAuctionTrackingPort;
  private final LoadClickUrlPort loadClickUrlPort;
  private final SendClickPort sendClickPort;
  private final Scheduler kafkaScheduler;

  @Override
  public Mono<String> handleClick(ClickCommand command) {
    return Mono.just(command)
        .filter(cmd -> cmd.auctionId() != null && !cmd.auctionId().isBlank())
        .flatMap(cmd -> loadAuctionTrackingPort.loadAuctionTracking(cmd.auctionId())
            .map(tracking -> new Click(
                cmd.auctionId(),
                tracking.requestId(),
                tracking.campaignId(),
                tracking.creativeId(),
                cmd.receivedAt()
            )))
        .doOnNext(click -> Mono.fromRunnable(() -> sendClickPort.sendClick(click))
            .subscribeOn(kafkaScheduler)
            .subscribe())
        .flatMap(click -> loadClickUrlPort.loadClickUrl(click.campaignId(), click.creativeId()));
  }
}
