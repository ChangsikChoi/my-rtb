package com.example.bidder.domain.port.in;

import com.example.bidder.domain.model.Win;
import reactor.core.publisher.Mono;

public interface WinUseCase {

  Mono<Win> handleWin(WinCommand command);
}
