package com.example.bidder.adapter.in.web.impression;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.port.in.ImpressionCommand;
import com.example.bidder.domain.port.in.ImpressionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ImpressionController.class)
class ImpressionControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private ImpressionUseCase impressionUseCase;

  @Test
  void shouldReturnNoContentWhenImpressionUseCaseReturnEmpty() {
    when(impressionUseCase.handleImpression(any(ImpressionCommand.class)))
        .thenReturn(Mono.empty());

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/imp")
                .queryParam("rid", "request123")
                .queryParam("cid", "campaign123")
                .queryParam("crid", "creative123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }

  @Test
  void shouldReturnNoContentWhenImpressionUseCaseReturnImpression() {
    Impression impression = mock(Impression.class);
    when(impressionUseCase.handleImpression(any(ImpressionCommand.class)))
        .thenReturn(Mono.just(impression));

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/imp")
                .queryParam("rid", "request123")
                .queryParam("cid", "campaign123")
                .queryParam("crid", "creative123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }
}