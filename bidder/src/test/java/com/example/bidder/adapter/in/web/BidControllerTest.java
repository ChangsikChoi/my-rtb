package com.example.bidder.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.bidder.adapter.in.web.bid.BidController;
import com.example.bidder.adapter.in.web.bid.BidWebMapper;
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
// WebFluxTest는 기본적으로 컨트롤러와 관련된 빈들만 로드하기 때문에, 필요한 매퍼 빈을 수동으로 등록해줘야 함
// @Import를 사용하면 특정 빈만 선택적으로 등록할 수 있어서 테스트 환경을 깔끔하게 유지할 수 있음
// TestConfiguration을 사용하면 여러 빈들을 한꺼번에 등록할 수 있지만, 설정이 복잡해질 수 있음
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