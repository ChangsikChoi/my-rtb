package com.example.bidder.adapter.in.web;

import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.in.BidUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/bid")
@RequiredArgsConstructor
public class BidController {

  private final BidUseCase bidUseCase;

  @PostMapping
  public Mono<ResponseEntity<BidResponseDto>> handleBid(@RequestBody BidRequestDto bidRequest) {
    BidCommand command = new BidCommand(
        bidRequest.getRequestId(),
        bidRequest.getRegion(),
        bidRequest.getBidfloor()
    );

    return bidUseCase.handleBidRequest(command)
        .map(response -> ResponseEntity.ok().body(
            new BidResponseDto(
                response.requestId(),
                response.price(),
                response.adMarkup(),
                response.winUrl())))
        .defaultIfEmpty(ResponseEntity.noContent().build());
  }

}
