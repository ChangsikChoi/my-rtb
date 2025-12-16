package com.example.bidder.adapter.in.web.win;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bidder.domain.model.Win;
import com.example.bidder.domain.port.in.WinCommand;
import com.example.bidder.domain.port.in.WinUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = WinController.class)
class WinControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private WinUseCase winUseCase;

  @Test
  void shouldReturnNoContentWhenWinUseCaseReturnEmpty() {
    when(winUseCase.handleWin(any(WinCommand.class)))
        .thenReturn(Mono.empty());

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/win")
                .queryParam("rid", "request123")
                .queryParam("cid", "campaign123")
                .queryParam("crid", "creative123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }

  @Test
  void shouldReturnNoContentWhenWinUseCaseReturnWin() {
    Win win = mock(Win.class);
    when(winUseCase.handleWin(any(WinCommand.class)))
        .thenReturn(Mono.just(win));

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/win")
                .queryParam("rid", "request123")
                .queryParam("cid", "campaign123")
                .queryParam("crid", "creative123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }

  @Test
  void shouldReturnNoContentWhenWinUseCaseReturnError() {
    when(winUseCase.handleWin(any(WinCommand.class)))
        .thenReturn(Mono.error(new RuntimeException("Test Exception")));

    webTestClient.get().uri(uriBuilder ->
            uriBuilder.path("/dsp/win")
                .queryParam("rid", "request123")
                .queryParam("cid", "campaign123")
                .queryParam("crid", "creative123")
                .build())
        .exchange()
        .expectStatus().isNoContent();
  }
}