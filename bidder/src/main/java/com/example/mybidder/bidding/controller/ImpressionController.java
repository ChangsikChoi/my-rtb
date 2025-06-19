package com.example.mybidder.bidding.controller;

import com.example.mybidder.bidding.service.ImpressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/dsp/imp")
@RequiredArgsConstructor
public class ImpressionController {

    private final ImpressionService impressionService;

    @GetMapping
    public Mono<ResponseEntity<Object>> handleImpression(@RequestParam String rid) {
        return impressionService.handleImpression(rid)
                .map(success -> success
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                )
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

    }
}
