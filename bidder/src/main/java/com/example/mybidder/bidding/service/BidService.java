package com.example.mybidder.bidding.service;

import com.example.KafkaBiddingLog;
import com.example.mybidder.bidding.model.BidRequest;
import com.example.mybidder.bidding.model.BidResponse;
import com.example.mybidder.bidding.redis.BudgetWithLuaScriptService;
import com.example.mybidder.bidding.redis.Campaign;
import com.example.mybidder.bidding.redis.RedisService;
import com.example.mybidder.bidding.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BudgetWithLuaScriptService budgetWithLuaScriptService;
    private final RedisService redisService;
    private final KafkaProducerService kafkaProducerService;

    public Mono<BidResponse> bid(BidRequest bidRequest) {
        Mono<Campaign> picked = redisService.getHighestCpmCampaign(bidRequest.region(), bidRequest.bidfloor());
        String requestId = bidRequest.requestId();
        return picked
                .flatMap(campaign ->
                        // 레디스 lua 스크립트를 사용한 예산 예약
                        budgetWithLuaScriptService.reserveBudget(
                                        campaign.id(),
                                        requestId,
                                        campaign.targetCpmMicro() / 1000,
                                        Duration.ofSeconds(30))
                                // 예산 예약 응답 생성
                                .flatMap(success -> {
                                    BigDecimal microToCpm = MicroConverter.convertMicroToCpm(campaign.targetCpmMicro());
                                    if (success) {
                                        kafkaProducerService.sendBidLog(
                                                KafkaBiddingLog.newBuilder()
                                                        .setRequestId(requestId)
                                                        .setCampaignId(campaign.id())
                                                        .setPrice(microToCpm.doubleValue())
                                                        .build());
                                        return Mono.just(new BidResponse(
                                                requestId,
                                                microToCpm,
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
