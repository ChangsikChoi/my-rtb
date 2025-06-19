package com.example.mybidder.bidding.controller;

import com.example.mybidder.bidding.service.WinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/win")
@RequiredArgsConstructor
public class WinController {

    private final WinService winService;

    @GetMapping
    public Mono<ResponseEntity<?>> handleWin(@RequestParam String rid) {
        winService.win(rid);
        return Mono.just(ResponseEntity.noContent().build());
    }
}
