package com.example.bidder.adapter.in.web.click;

import com.example.bidder.domain.port.in.ClickCommand;
import com.example.bidder.domain.port.in.ClickUseCase;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
  public Mono<Void> handleClick(ServerWebExchange exchange, ClickRequestDto clickRequest) {
    ClickCommand clickCommand = new ClickCommand(clickRequest.rid(), clickRequest.cid(),
        clickRequest.crid());

    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
    exchange.getResponse().getHeaders().setLocation(URI.create(clickRequest.url()));

    // TODO: Functional Endpoint 적용 시 사용
    // ServerResponse.temporaryRedirect(URI.create(clickRequest.url())).build()

    return clickUseCase.handleClick(clickCommand)
        .onErrorResume(e -> Mono.empty())
        .then(exchange.getResponse().setComplete());
  }
}
