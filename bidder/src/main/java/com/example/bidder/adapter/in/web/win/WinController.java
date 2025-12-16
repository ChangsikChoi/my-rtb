package com.example.bidder.adapter.in.web.win;

import com.example.bidder.domain.port.in.WinCommand;
import com.example.bidder.domain.port.in.WinUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/win")
@RequiredArgsConstructor
public class WinController {

  private final WinUseCase winUseCase;

  @GetMapping
  public Mono<ResponseEntity<Object>> handleWin(WinRequestDto winRequest) {
    WinCommand command = new WinCommand(winRequest.rid(), winRequest.cid(), winRequest.crid());

    return winUseCase.handleWin(command)
        .thenReturn(ResponseEntity.noContent().build())
        .onErrorReturn(ResponseEntity.noContent().build());
  }
}
