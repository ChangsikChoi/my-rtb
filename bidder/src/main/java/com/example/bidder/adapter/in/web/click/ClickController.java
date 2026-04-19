package com.example.bidder.adapter.in.web.click;

import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.in.ClickUseCase;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/redirect")
@RequiredArgsConstructor
public class ClickController {

  private final ClickUseCase clickUseCase;

  @GetMapping
  public Mono<Void> handleClick(
      ServerWebExchange exchange,
      @RequestParam("aid") String auctionId
  ) {
    return clickUseCase.handleClick(new ClickCommand(auctionId, System.currentTimeMillis()))
        .flatMap(clickUrl -> redirect(exchange, clickUrl))
        .switchIfEmpty(noContent(exchange))
        .onErrorResume(e -> noContent(exchange));
  }

  private Mono<Void> redirect(ServerWebExchange exchange, String clickUrl) {
    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
    exchange.getResponse().getHeaders().setLocation(URI.create(clickUrl));
    return exchange.getResponse().setComplete();
  }

  private Mono<Void> noContent(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
    return exchange.getResponse().setComplete();
  }
}
