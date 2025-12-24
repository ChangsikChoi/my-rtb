package com.example.bidder.domain.port.in;

import com.example.bidder.domain.model.Click;
import reactor.core.publisher.Mono;

public interface ClickUseCase {

  Mono<Click> handleClick(ClickCommand command);
}
