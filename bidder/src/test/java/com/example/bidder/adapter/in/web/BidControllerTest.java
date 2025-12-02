package com.example.bidder.adapter.in.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.port.in.BidCommand;
import com.example.bidder.domain.port.in.BidUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = BidController.class)
@Import(BidWebMapper.class)
class BidControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private BidUseCase bidUseCase;

  @Test
  void shouldReturnBidSuccessResponse() {
    String requestJson = "{\"id\": \"test123\", \"imp\": {\"bidFloor\": 10}}";
    Bid successBid = Bid.builder()
        .requestId("test123")
        .campaignId("campaign1")
        .creativeId("creative1")
        .bidPriceCpmMicro(10_000_000L)
        .winUrl("http://example.com/win")
        .adMarkup("<ad>Test Ad</ad>")
        .build();

    when(bidUseCase.handleBidRequest(any(BidCommand.class)))
        .thenReturn(Mono.just(successBid));

    webTestClient.post().uri("/dsp/bid")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestJson)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.requestId").isEqualTo("test123")
        .jsonPath("$.price").isEqualTo(10)
        .jsonPath("$.winUrl").isEqualTo("http://example.com/win");

  }

  @Test
  void shouldReturnNoContentWhenNoBid() {
    String requestJson = "{\"id\": \"test123\", \"imp\": {\"bidFloor\": 10}}";

    when(bidUseCase.handleBidRequest(any(BidCommand.class)))
        .thenReturn(Mono.empty());

    webTestClient.post().uri("/dsp/bid")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(requestJson)
        .exchange()
        .expectStatus().isNoContent();
  }
}