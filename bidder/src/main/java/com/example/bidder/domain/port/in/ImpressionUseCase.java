package com.example.bidder.domain.port.in;

import com.example.bidder.domain.model.Impression;
import reactor.core.publisher.Mono;

public interface ImpressionUseCase {

  Mono<Impression> handleImpression(ImpressionCommand command);
}
