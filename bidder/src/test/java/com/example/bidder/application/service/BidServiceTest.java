package com.example.bidder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

  @Mock
  private BudgetReservePort budgetReservePort;
  @Mock
  private LoadCampaignPort loadCampaignPort;
  @Mock
  private SendBidResultPort sendBidResultPort;

  @InjectMocks
  private BidService bidService;

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
        .width(300)
        .height(250)
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
          assertThat(bid.campaignId()).isEqualTo("camp_test");
          assertThat(bid.bidPriceCpmMicro()).isEqualTo(100_000L);
        })
        .verifyComplete();

    verify(budgetReservePort, times(1))
        .reserveBudget("camp_test", "req_test", 100_000L);
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
        .width(300)
        .height(250)
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

    verify(sendBidResultPort, never()).sendBidResult(any());
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

    verifyNoInteractions(budgetReservePort, sendBidResultPort);
  }
}