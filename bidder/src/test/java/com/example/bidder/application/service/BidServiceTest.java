package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;

import com.example.bidder.application.support.AuctionIdGenerator;
import com.example.bidder.domain.model.AuctionTracking;
import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.model.BidRequest;
import com.example.bidder.domain.model.Campaign;
import com.example.bidder.domain.model.Creative;
import com.example.bidder.domain.model.Imp;
import com.example.bidder.domain.model.Target;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.out.BudgetReservePort;
import com.example.bidder.domain.port.out.LoadCampaignPort;
import com.example.bidder.domain.port.out.SendBidResultPort;
import com.example.bidder.domain.port.out.StoreAuctionTrackingPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

  @Mock
  private BudgetReservePort budgetReservePort;
  @Mock
  private LoadCampaignPort loadCampaignPort;
  @Mock
  private StoreAuctionTrackingPort storeAuctionTrackingPort;
  @Mock
  private SendBidResultPort sendBidResultPort;
  @Mock
  private AuctionIdGenerator auctionIdGenerator;

  private final Scheduler kafkaScheduler = Schedulers.immediate();

  private BidService bidService;

  @BeforeEach
  void setUp() {
    bidService = new BidService(
        budgetReservePort,
        loadCampaignPort,
        storeAuctionTrackingPort,
        sendBidResultPort,
        auctionIdGenerator,
        kafkaScheduler
    );
    when(auctionIdGenerator.generate()).thenReturn("auction_test");
    lenient().when(storeAuctionTrackingPort.storeAuctionTracking(any(AuctionTracking.class))).thenReturn(
        Mono.empty());
  }

  @Test
  void givenEligibleCampaign_whenReserveBudgetSuccess_thenReturnBidAndSendResult() {
    BidRequest bidRequest = BidRequest.builder()
        .id("req_test")
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .bidFloorMicro(50_000L)
            .build())
        .build();

    Target target = Target.builder().build();
    Creative creative = Creative.builder()
        .id("creative_test")
        .width(300)
        .height(250)
        .imageUrl("http://example.com/image.png")
        .clickUrl("http://example.com/click")
        .build();
    Campaign winner = Campaign.builder()
        .id("camp_test")
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(100_000L)
        .target(target)
        .creative(creative)
        .build();

    when(loadCampaignPort.loadCampaign()).thenReturn(Flux.just(winner));
    when(budgetReservePort.reserveBudget(any(), any(), anyLong())).thenReturn(Mono.just(true));
    doNothing().when(sendBidResultPort).sendBidResult(any());

    BidCommand command = mock(BidCommand.class);
    when(command.toDomain()).thenReturn(bidRequest);

    Mono<Bid> result = bidService.handleBidRequest(command);

    StepVerifier.create(result)
        .assertNext(bid -> {
          assertThat(bid.auctionId()).isEqualTo("auction_test");
          assertThat(bid.campaignId()).isEqualTo("camp_test");
          assertThat(bid.bidPriceCpmMicro()).isEqualTo(100_000L);
          assertThat(bid.winUrl()).contains("aid=auction_test");
          assertThat(bid.adMarkup()).contains("/dsp/imp?aid=auction_test");
          assertThat(bid.adMarkup()).contains("/dsp/redirect?aid=auction_test");
        })
        .verifyComplete();

    verify(budgetReservePort, times(1))
        .reserveBudget("camp_test", "auction_test", 100L);
    verify(storeAuctionTrackingPort, times(1)).storeAuctionTracking(argThat(tracking ->
        "auction_test".equals(tracking.auctionId())
            && "req_test".equals(tracking.requestId())
            && "camp_test".equals(tracking.campaignId())
            && "creative_test".equals(tracking.creativeId())
            && Long.valueOf(100L).equals(tracking.priceMicro())
            && tracking.receivedAt() != null
    ));
    verify(sendBidResultPort, times(1)).sendBidResult(any());
  }

  @Test
  void givenEligibleCampaign_whenReserveBudgetFails_thenReturnEmptyMono() {
    // given
    BidRequest bidRequest = BidRequest.builder()
        .id("req_test")
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .bidFloorMicro(50_000L)
            .build())
        .build();

    Target target = Target.builder().build();
    Creative creative = Creative.builder()
        .id("creative_test")
        .width(300)
        .height(250)
        .imageUrl("http://example.com/image.png")
        .clickUrl("http://example.com/click")
        .build();
    Campaign winner = Campaign.builder()
        .id("camp_test")
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(100_000L)
        .target(target)
        .creative(creative)
        .build();

    when(loadCampaignPort.loadCampaign()).thenReturn(Flux.just(winner));
    when(budgetReservePort.reserveBudget(any(), any(), anyLong())).thenReturn(Mono.just(false));

    BidCommand command = mock(BidCommand.class);
    when(command.toDomain()).thenReturn(bidRequest);

    Mono<Bid> result = bidService.handleBidRequest(command);

    StepVerifier.create(result)
        .verifyComplete(); // 결과 없음

    verify(storeAuctionTrackingPort, never()).storeAuctionTracking(any(AuctionTracking.class));
    verify(sendBidResultPort, never()).sendBidResult(any());
  }

  @Test
  void givenTopCampaignReserveFails_whenNextCampaignReserveSucceeds_thenReturnFallbackBid() {
    BidRequest bidRequest = BidRequest.builder()
        .id("req_test")
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .bidFloorMicro(50_000L)
            .build())
        .build();

    Target target = Target.builder().build();
    Creative creative = Creative.builder()
        .id("creative_test")
        .width(300)
        .height(250)
        .imageUrl("http://example.com/image.png")
        .clickUrl("http://example.com/click")
        .build();

    Campaign secondCampaign = Campaign.builder()
        .id("camp_second")
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(150_000L)
        .target(target)
        .creative(creative)
        .build();

    Campaign topCampaign = Campaign.builder()
        .id("camp_top")
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(200_000L)
        .target(target)
        .creative(creative)
        .build();

    when(loadCampaignPort.loadCampaign()).thenReturn(Flux.just(secondCampaign, topCampaign));
    when(budgetReservePort.reserveBudget("camp_top", "auction_test", 200L))
        .thenReturn(Mono.just(false));
    when(budgetReservePort.reserveBudget("camp_second", "auction_test", 150L))
        .thenReturn(Mono.just(true));
    doNothing().when(sendBidResultPort).sendBidResult(any());

    BidCommand command = mock(BidCommand.class);
    when(command.toDomain()).thenReturn(bidRequest);

    Mono<Bid> result = bidService.handleBidRequest(command);

    StepVerifier.create(result)
        .assertNext(bid -> {
          assertThat(bid.auctionId()).isEqualTo("auction_test");
          assertThat(bid.campaignId()).isEqualTo("camp_second");
          assertThat(bid.bidPriceCpmMicro()).isEqualTo(150_000L);
        })
        .verifyComplete();

    InOrder inOrder = inOrder(budgetReservePort);
    inOrder.verify(budgetReservePort).reserveBudget("camp_top", "auction_test", 200L);
    inOrder.verify(budgetReservePort).reserveBudget("camp_second", "auction_test", 150L);
    verify(storeAuctionTrackingPort, times(1)).storeAuctionTracking(argThat(tracking ->
        "auction_test".equals(tracking.auctionId())
            && "camp_second".equals(tracking.campaignId())
            && "creative_test".equals(tracking.creativeId())
            && Long.valueOf(150L).equals(tracking.priceMicro())
    ));
    verify(sendBidResultPort, times(1)).sendBidResult(any());
  }

  @Test
  void givenTopCampaignReserveSucceeds_whenOtherCampaignsExist_thenDoNotReserveOthers() {
    BidRequest bidRequest = BidRequest.builder()
        .id("req_test")
        .imp(Imp.builder()
            .width(300)
            .height(250)
            .bidFloorMicro(50_000L)
            .build())
        .build();

    Target target = Target.builder().build();
    Creative creative = Creative.builder()
        .id("creative_test")
        .width(300)
        .height(250)
        .imageUrl("http://example.com/image.png")
        .clickUrl("http://example.com/click")
        .build();

    Campaign lowerCampaign = Campaign.builder()
        .id("camp_lower")
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(150_000L)
        .target(target)
        .creative(creative)
        .build();

    Campaign topCampaign = Campaign.builder()
        .id("camp_top")
        .startDate(LocalDateTime.now().minusDays(1))
        .endDate(LocalDateTime.now().plusDays(1))
        .remainingBudgetMicro(1_000_000L)
        .targetCpmMicro(200_000L)
        .target(target)
        .creative(creative)
        .build();

    when(loadCampaignPort.loadCampaign()).thenReturn(Flux.just(lowerCampaign, topCampaign));
    when(budgetReservePort.reserveBudget("camp_top", "auction_test", 200L))
        .thenReturn(Mono.just(true));
    doNothing().when(sendBidResultPort).sendBidResult(any());

    BidCommand command = mock(BidCommand.class);
    when(command.toDomain()).thenReturn(bidRequest);

    Mono<Bid> result = bidService.handleBidRequest(command);

    StepVerifier.create(result)
        .assertNext(bid -> assertThat(bid.campaignId()).isEqualTo("camp_top"))
        .verifyComplete();

    verify(budgetReservePort, times(1)).reserveBudget("camp_top", "auction_test", 200L);
    verify(budgetReservePort, never()).reserveBudget("camp_lower", "auction_test", 150L);
    verify(storeAuctionTrackingPort, times(1)).storeAuctionTracking(any(AuctionTracking.class));
    verify(sendBidResultPort, times(1)).sendBidResult(any());
  }


  @Test
  void givenNoCampaigns_whenHandleBidRequest_thenReturnEmptyMono() {
    // given
    BidCommand command = mock(BidCommand.class);
    when(command.toDomain()).thenReturn(BidRequest.builder().build());
    when(loadCampaignPort.loadCampaign()).thenReturn(Flux.empty());

    // when
    Mono<Bid> result = bidService.handleBidRequest(command);

    // then
    StepVerifier.create(result)
        .verifyComplete();

    verifyNoInteractions(budgetReservePort, storeAuctionTrackingPort, sendBidResultPort);
  }
}
