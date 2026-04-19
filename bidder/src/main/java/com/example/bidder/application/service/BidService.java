package com.example.bidder.application.service;

import com.example.bidder.application.support.AuctionIdGenerator;
import com.example.bidder.domain.model.AuctionTracking;
import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.model.BidRequest;
import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.in.BidUseCase;
import com.example.bidder.domain.port.out.BudgetReservePort;
import com.example.bidder.domain.port.out.LoadCampaignPort;
import com.example.bidder.domain.port.out.SendBidResultPort;
import com.example.bidder.domain.port.out.StoreAuctionTrackingPort;
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
  private final StoreAuctionTrackingPort storeAuctionTrackingPort;
  private final SendBidResultPort sendBidResultPort;
  private final AuctionIdGenerator auctionIdGenerator;
  private final Scheduler kafkaScheduler;

  private final CampaignRankingService campaignRankingService = new CampaignRankingService();

  @Override
  public Mono<Bid> handleBidRequest(BidCommand command) {
    BidRequest bidRequest = command.toDomain();
    String auctionId = auctionIdGenerator.generate();

    return rankedCampaigns(bidRequest)
        .concatMap(campaign -> reserveAndBuildBid(campaign, bidRequest, auctionId, command.receivedAt()), 1)
        .next()
        .flatMap(bid -> storeAuctionTracking(buildAuctionTracking(bid)).thenReturn(bid))
        .doOnNext(this::publishBidResultAsync);
  }

  private Flux<Campaign> rankedCampaigns(BidRequest bidRequest) {
    return campaignRankingService.rankEligibleCampaigns(
        loadCampaignPort.loadCampaign(),
        bidRequest
    );
  }

  private Mono<Bid> reserveAndBuildBid(
      Campaign campaign,
      BidRequest bidRequest,
      String auctionId,
      long receivedAt
  ) {
    long priceMicro = campaign.impressionPriceMicro();

    return budgetHandlePort.reserveBudget(campaign.id(), auctionId, priceMicro)
        .flatMap(reserved -> reserved
            ? Mono.just(buildBidResult(auctionId, bidRequest, campaign, receivedAt))
            : Mono.empty());
  }

  private void publishBidResultAsync(Bid bid) {
    Mono.fromRunnable(() -> sendBidResultPort.sendBidResult(bid))
        .subscribeOn(kafkaScheduler)
        .subscribe();
  }

  private Mono<Void> storeAuctionTracking(AuctionTracking auctionTracking) {
    return storeAuctionTrackingPort.storeAuctionTracking(auctionTracking);
  }

  private AuctionTracking buildAuctionTracking(Bid bid) {
    return AuctionTracking.builder()
        .auctionId(bid.auctionId())
        .requestId(bid.requestId())
        .campaignId(bid.campaignId())
        .creativeId(bid.creativeId())
        .priceMicro(bid.impressionPriceMicro())
        .receivedAt(bid.receivedAt())
        .build();
  }

  private Bid buildBidResult(String auctionId, BidRequest bidRequest, Campaign campaign, long receivedAt) {
    String adMarkup = buildAdMarkup(auctionId, campaign);
    String winUrl = buildWinUrl(auctionId);

    return Bid.builder()
        .auctionId(auctionId)
        .requestId(bidRequest.id())
        .campaignId(campaign.id())
        .creativeId(campaign.creative().id())
        .bidPriceCpmMicro(campaign.targetCpmMicro())
        .receivedAt(receivedAt)
        .adMarkup(adMarkup)
        .winUrl(winUrl)
        .build();
  }

  private String buildWinUrl(String auctionId) {
    return "http://localhost:8080/dsp/win?aid=" + auctionId;
  }

  private String buildAdMarkup(String auctionId, Campaign campaign) {
    StringBuilder adMarkupBuilder = new StringBuilder();
    // div 태그 오픈
    adMarkupBuilder.append("<div>");
    // div > img 태그 (노출 확인 url 설정)
    adMarkupBuilder.append("<img src='http://localhost:8080/dsp/imp?aid=")
        .append(auctionId)
        .append("' height=1 width=1 style='display:none;'/>");
    // div > a 태그 오픈 (클릭 링크 설정)
    adMarkupBuilder.append("<a href='http://localhost:8080/dsp/redirect?aid=")
        .append(auctionId)
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
