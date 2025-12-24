package com.example.bidder.application.service;

import com.example.bidder.domain.model.Click;
import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.in.ClickUseCase;
import com.example.bidder.domain.port.out.SendClickPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ClickService implements ClickUseCase {

  private final SendClickPort sendClickPort;

  @Override
  public Mono<Click> handleClick(ClickCommand command) {
    return Mono.just(command)
        .filter(cmd -> cmd.requestId() != null && !cmd.requestId().isBlank())
        .map(cmd -> {
          Click click = new Click(cmd.requestId(), cmd.campaignId(), cmd.creativeId());
          sendClickPort.sendClick(click);
          return click;
        });
  }
}
