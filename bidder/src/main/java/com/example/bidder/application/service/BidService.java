package com.example.bidder.application.service;

import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.model.BidRequest;
import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.in.BidUseCase;
import com.example.bidder.domain.port.out.BudgetReservePort;
import com.example.bidder.domain.port.out.LoadCampaignPort;
import com.example.bidder.domain.port.out.SendBidResultPort;
import com.example.bidder.domain.service.CampaignRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
public class BidService implements BidUseCase {

  private final BudgetReservePort budgetHandlePort;
  private final LoadCampaignPort loadCampaignPort;
  private final SendBidResultPort sendBidResultPort;
  private final Scheduler kafkaScheduler;

  private final CampaignRankingService campaignRankingService = new CampaignRankingService();

  @Override
  public Mono<Bid> handleBidRequest(BidCommand command) {
    BidRequest bidRequest = command.toDomain();
    return rankedCampaigns(bidRequest)
        .concatMap(campaign -> reserveAndBuildBid(campaign, bidRequest), 1)
        .next()
        .doOnNext(this::publishBidResultAsync);
  }

  private Flux<Campaign> rankedCampaigns(BidRequest bidRequest) {
    return campaignRankingService.rankEligibleCampaigns(
        loadCampaignPort.loadCampaign(),
        bidRequest
    );
  }

  private Mono<Bid> reserveAndBuildBid(Campaign campaign, BidRequest bidRequest) {
    return budgetHandlePort.reserveBudget(
            campaign.id(),
            bidRequest.id(),
            campaign.impressionPriceMicro()
        )
        .filter(Boolean::booleanValue)
        .map(ignored -> buildBidResult(bidRequest, campaign));
  }

  private void publishBidResultAsync(Bid bid) {
    Mono.fromRunnable(() -> sendBidResultPort.sendBidResult(bid))
        .subscribeOn(kafkaScheduler)
        .subscribe();
  }

  private Bid buildBidResult(BidRequest bidRequest, Campaign campaign) {
    String adMarkup = buildAdMarkup(bidRequest, campaign);
    String winUrl =
        "http://localhost:8080/dsp/win?rid=" + bidRequest.id()
            + "&cid=" + campaign.id()
            + "&crid=" + campaign.creative().id();

    return Bid.builder()
        .requestId(bidRequest.id())
        .campaignId(campaign.id())
        .creativeId(campaign.creative().id())
        .bidPriceCpmMicro(campaign.targetCpmMicro())
        .adMarkup(adMarkup)
        .winUrl(winUrl)
        .build();
  }

  private String buildAdMarkup(BidRequest bidRequest, Campaign campaign) {
    StringBuilder adMarkupBuilder = new StringBuilder();
    // div 태그 오픈
    adMarkupBuilder.append("<div>");
    // div > img 태그 (노출 확인 url 설정)
    adMarkupBuilder.append("<img src='http://localhost:8080/dsp/imp?rid=")
        .append(bidRequest.id())
        .append("&cid=")
        .append(campaign.id())
        .append("&crid=")
        .append(campaign.creative().id())
        .append("' height=1 width=1 style='display:none;'/>");
    // div > a 태그 오픈 (클릭 링크 설정)
    adMarkupBuilder.append("<a href='http://localhost:8080/dsp/redirect?url=")
        .append(campaign.creative().clickUrl())
        .append("&rid=")
        .append(bidRequest.id())
        .append("&cid=")
        .append(campaign.id())
        .append("&crid=")
        .append(campaign.creative().id())
        .append("' target='_blank' >");
    // div > a > img 태그 오픈 (광고 이미지 설정)
    adMarkupBuilder.append("<img src='")
        .append(campaign.creative().imageUrl())
        .append("' ");
    // 크기 설정
    if (campaign.creative().width() != null) {
      adMarkupBuilder.append("width='")
          .append(campaign.creative().width())
          .append("' ");
    }
    if (campaign.creative().height() != null) {
      adMarkupBuilder.append("height='")
          .append(campaign.creative().height())
          .append("' ");
    }
    // div > a > img 태그 닫기 (광고 이미지 설정)
    adMarkupBuilder.append("/>")
        // div > a 태그 닫기 (클릭 링크 설정)
        .append("</a>")
        // div 태그 닫기
        .append("</div>");

    return adMarkupBuilder.toString();
  }
}
