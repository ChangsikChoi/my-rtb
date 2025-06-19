package com.example.mybidder.bidding.controller;

import com.example.mybidder.bidding.model.BidRequest;
import com.example.mybidder.bidding.model.BidResponse;
import com.example.mybidder.bidding.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/bid")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    public Mono<ResponseEntity<BidResponse>> handleBid(@RequestBody BidRequest bidRequest) {
        return bidService.bid(bidRequest)
                .map(response -> ResponseEntity.ok().body(response))
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

}
