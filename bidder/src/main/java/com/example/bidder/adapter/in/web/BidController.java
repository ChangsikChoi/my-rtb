package com.example.bidder.adapter.in.web;

import com.example.bidder.application.BidRequestVo;
import com.example.bidder.application.BidResponseVo;
import com.example.bidder.application.service.BidService;
import com.example.bidder.domain.model.Bid;
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
    public Mono<ResponseEntity<BidResponseDto>> handleBid(@RequestBody BidRequestVo bidRequest) {
        // 1. Web DTO를 Application 계층이 이해하는 Command 객체로 변환
        BidCommand command = new BidCommand(
                new CampaignId(requestDto.getCampaignId()),
                requestDto.getPrice()
        );

        // 2. UseCase 실행
        return bidUseCase.handleBidRequest(command)
                //TODO: Application 결과를 Web DTO로 변환하여 응답
//                .map(response -> ResponseEntity.ok().body(response))
                .map(response -> ResponseEntity.ok().body(new BidResponseDto()))
                .defaultIfEmpty(ResponseEntity.noContent().build());;

    }

}
