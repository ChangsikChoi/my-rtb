package com.example.bidder.adapter.in.web.bid;

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
  private final BidWebMapper bidWebMapper;

  @PostMapping
  public Mono<ResponseEntity<BidResponseDto>> handleBid(@RequestBody BidRequestDto bidRequest) {
    BidCommand command = bidWebMapper.toCommand(bidRequest);

    return bidUseCase.handleBidRequest(command)
        .map(response -> ResponseEntity.ok().body(bidWebMapper.toDto(response)))
        .defaultIfEmpty(ResponseEntity.noContent().build());
  }

}
