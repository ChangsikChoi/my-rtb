package com.example.bidder.application.service;

import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.port.in.ImpressionCommand;
import com.example.bidder.domain.port.in.ImpressionUseCase;
import com.example.bidder.domain.port.out.SendImpressionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImpressionService implements ImpressionUseCase {

  private final SendImpressionPort sendImpressionPort;

  @Override
  public Mono<Impression> handleImpression(ImpressionCommand command) {
    return Mono.just(command)
        .filter(cmd -> cmd.requestId() != null && !cmd.requestId().isBlank())
        .map(cmd -> new Impression(cmd.requestId(), cmd.campaignId(),
            cmd.creativeId()))
        .doOnNext(sendImpressionPort::sendImpression);
  }
}
