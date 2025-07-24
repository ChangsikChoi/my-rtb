package com.example.bidder.application.service;

import com.example.bidder.application.BidRequestCommand;
import com.example.bidder.application.BidResponseVo;
import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.in.BidUseCase;
import com.example.bidder.domain.port.out.BudgetReservePort;
import com.example.bidder.domain.port.out.LoadCampaignPort;
import com.example.bidder.domain.port.out.SendBidResultPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BidService implements BidUseCase {

  private final BudgetReservePort budgetHandlePort;
  private final LoadCampaignPort loadCampaignPort;
  private final SendBidResultPort sendBidResultPort;

  //TODO: 테스트 코드 작성 하면서 삭제
  public Mono<BidResponseVo> bid(BidRequestCommand bidRequest) {
    return Mono.empty();
  }

  private String getWinUrl(BidCommand command) {
    return "http://localhost:8080/dsp/win?rid=" + command.requestId();
  }

  private String getAdMarkup(BidCommand command) {
    return "<img src='http://localhost:8080/dsp/imp?rid=" + command.requestId() + " />";
  }

  @Override
  public Mono<Bid> handleBidRequest(BidCommand command) {
    Mono<Campaign> picked = loadCampaignPort.loadCampaign(command.region(), command.bidfloor());
    String requestId = command.requestId();
    return picked
        .flatMap(campaign ->
            // 레디스 lua 스크립트를 사용한 예산 예약
            budgetHandlePort.reserveBudget(
                    campaign.id(),
                    requestId,
                    campaign.targetCpm(),
                    Duration.ofSeconds(30))
                // 예산 예약 응답 생성
                .flatMap(success -> {
                  if (success) {
                    Bid bidResult = new Bid(
                        requestId,
                        campaign.id(),
                        campaign.targetCpm(),
                        getAdMarkup(command),
                        getWinUrl(command));
                    sendBidResultPort.sendBidResult(bidResult);
                    return Mono.just(bidResult);
                  } else {
                    return Mono.empty();
                  }
                })
        );
  }
}
