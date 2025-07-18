package com.example.bidder.application.service;

import com.example.bidder.application.BidRequestVo;
import com.example.bidder.application.BidResponseVo;
import com.example.bidder.adapter.out.redis.BudgetWithLuaScriptService;
import com.example.bidder.adapter.out.redis.Campaign;
import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.in.BidUseCase;
import com.example.bidder.domain.port.out.LoadCampaignPort;
import com.example.bidder.domain.port.out.SendBidResultPort;
import com.example.bidder.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BidService implements BidUseCase {

    //TODO: 포트-어댑터 적용.
    // 리액터 흐름 내에서 끊기지 않고 사용 가능한 방법 탐색 및 적용
    private final BudgetWithLuaScriptService budgetWithLuaScriptService;

    private final LoadCampaignPort loadCampaignPort;
    private final SendBidResultPort sendBidResultPort;

    public Mono<BidResponseVo> bid(BidRequestVo bidRequest) {
        // TODO: 입찰에 적절한 캠페인을 가져오는 것임을 알 수 있도록 메소드명 수정 ( 필요시 포트 이름도 수정 )
        Mono<Campaign> picked = loadCampaignPort.loadCampaign(bidRequest.region(), bidRequest.bidfloor());
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
                                        sendBidResultPort.sendBidResult(
//                                                KafkaBiddingLog.newBuilder()
//                                                        .setRequestId(requestId)
//                                                        .setCampaignId(campaign.id())
//                                                        .setPrice(microToCpm.doubleValue())
//                                                        .build()
                                                //TODO: 메세지로 보낼 비딩 결과 객체 설정
                                                new Bid()
                                        );
                                        return Mono.just(new BidResponseVo(
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

    private String getWinUrl(BidRequestVo bidRequest) {
        return "http://localhost:8080/dsp/win?rid=" + bidRequest.requestId();
    }

    private String getAdMarkup(BidRequestVo bidRequest) {
        return "<img src='http://localhost:8080/dsp/imp?rid=" + bidRequest.requestId() + " />";
    }

    //TODO: 기존 bid 메소드 로직 이식
    @Override
    public Mono<Bid> handleBidRequest(BidCommand command) {
        return null;
    }
}
