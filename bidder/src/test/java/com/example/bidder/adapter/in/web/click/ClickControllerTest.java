package com.example.bidder.adapter.in.web.click;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.in.ClickUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ClickController.class)
class ClickControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private ClickUseCase clickUseCase;

  @Test
  void shouldReturnFoundWhenHandleClickReturnsClickUrl() {
    when(clickUseCase.handleClick(any(ClickCommand.class))).thenReturn(Mono.just("http://example.com"));

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/redirect")
                .queryParam("aid", "auction123")
                .build())
        .exchange()
        .expectStatus().isFound()
        .expectHeader().location("http://example.com");
  }

  @Test
  void shouldReturnNoContentWhenHandleClickReturnsError() {
    when(clickUseCase.handleClick(any(ClickCommand.class)))
        .thenReturn(Mono.error(new RuntimeException("Test Exception")));

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/redirect")
                .queryParam("aid", "auction123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }

  @Test
  void shouldReturnNoContentWhenHandleClickReturnsEmpty() {
    when(clickUseCase.handleClick(any(ClickCommand.class))).thenReturn(Mono.empty());

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/redirect")
                .queryParam("aid", "auction123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }
}
