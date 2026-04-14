package com.example.bidder.adapter.in.web.impression;

import com.example.bidder.domain.port.in.ImpressionCommand;
import com.example.bidder.domain.port.in.ImpressionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/imp")
@RequiredArgsConstructor
public class ImpressionController {

  private final ImpressionUseCase impressionUseCase;

  @GetMapping
  public Mono<ResponseEntity<Object>> handleImpression(@RequestParam("aid") String auctionId) {
    return impressionUseCase.handleImpression(new ImpressionCommand(auctionId))
        .thenReturn(ResponseEntity.noContent().build())
        .onErrorReturn(ResponseEntity.noContent().build());

  }
}
