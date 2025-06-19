package com.example.mybidder.bidding.service;


import com.example.mybidder.bidding.model.BidRequest;
import com.example.mybidder.bidding.model.BidResponse;
import com.example.mybidder.bidding.redis.BudgetWithLuaScriptService;
import com.example.mybidder.bidding.redis.Campaign;
import com.example.mybidder.bidding.redis.RedisService;
import com.example.mybidder.bidding.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BudgetWithLuaScriptService budgetWithLuaScriptService;
    private final RedisService redisService;

    public Mono<BidResponse> bid(BidRequest bidRequest) {
        Mono<Campaign> picked = redisService.getHighestCpmCampaign(bidRequest.region(), bidRequest.bidfloor());

        return picked
                .flatMap(campaign ->
                // 레디스 lua 스크립트를 사용한 예산 예약
                budgetWithLuaScriptService.reserveBudget(
                                campaign.id(),
                                bidRequest.requestId(),
                                campaign.targetCpmMicro() / 1000,
                                Duration.ofSeconds(30))
                        // 예산 예약 응답 생성
                        .flatMap(success -> {
                            if (success) {
                                return Mono.just(new BidResponse(
                                        bidRequest.requestId(),
                                        MicroConverter.convertMicroToCpm(campaign.targetCpmMicro()),
                                        getAdMarkup(bidRequest),
                                        getWinUrl(bidRequest)));
                            } else {
                                return Mono.empty();
                            }
                        })
        );
    }

    private String getWinUrl(BidRequest bidRequest) {
        return "http://localhost:8080/dsp/win?rid=" + bidRequest.requestId();
    }

    private String getAdMarkup(BidRequest bidRequest) {
        return "<img src='http://localhost:8080/dsp/imp?rid=" + bidRequest.requestId() + " />";
    }

}
