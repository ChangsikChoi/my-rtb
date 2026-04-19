package com.example.bidder.domain.port.in;

import reactor.core.publisher.Mono;

public interface ClickUseCase {

  Mono<String> handleClick(ClickCommand command);
}
