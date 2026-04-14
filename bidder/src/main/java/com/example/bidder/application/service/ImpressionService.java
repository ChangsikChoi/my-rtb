package com.example.bidder.application.service;

import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.port.in.ImpressionCommand;
import com.example.bidder.domain.port.in.ImpressionUseCase;
import com.example.bidder.domain.port.out.LoadAuctionTrackingPort;
import com.example.bidder.domain.port.out.SendImpressionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
public class ImpressionService implements ImpressionUseCase {

  private final LoadAuctionTrackingPort loadAuctionTrackingPort;
  private final SendImpressionPort sendImpressionPort;
  private final Scheduler kafkaScheduler;

  @Override
  public Mono<Impression> handleImpression(ImpressionCommand command) {
    return Mono.just(command)
        .filter(cmd -> cmd.auctionId() != null && !cmd.auctionId().isBlank())
        .flatMap(cmd -> loadAuctionTrackingPort.loadAuctionTracking(cmd.auctionId())
            .map(tracking -> new Impression(
                cmd.auctionId(),
                tracking.requestId(),
                tracking.campaignId(),
                tracking.creativeId()
            )))
        .doOnNext(
            impression -> Mono.fromRunnable(() -> sendImpressionPort.sendImpression(impression))
                .subscribeOn(kafkaScheduler)
                .subscribe());
  }
}
