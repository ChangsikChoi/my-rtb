package com.example.mybidder.controller;

import com.example.bidder.adapter.in.web.BidController;
import com.example.bidder.application.BidRequestVo;
import com.example.bidder.application.BidResponseVo;
import com.example.bidder.application.service.BidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BidControllerTest {
    private WebTestClient webTestClient;
    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = mock(BidService.class);
        webTestClient = WebTestClient.bindToController(new BidController(bidService)).build();
    }

    @Test
    void 입찰_성공() {
        BidResponseVo bidResponse = new BidResponseVo(
                "test",
                new BigDecimal(100),
                "http://localhost:8080/dsp/win?rid=test",
                "<img src='http://localhost:8080/dsp/imp?rid=test />"
        );
        BidRequestVo bidRequest = new BidRequestVo("test", "seoul", new BigDecimal(99));
        when(bidService.bid(bidRequest)).thenReturn(Mono.just(bidResponse));

        webTestClient.post().uri("/dsp/bid")
                .bodyValue(bidRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void 입찰_실패() {
        BidRequestVo bidRequest = new BidRequestVo("test", "seoul", new BigDecimal(99));
        when(bidService.bid(bidRequest)).thenReturn(Mono.empty());

        webTestClient.post().uri("/dsp/bid")
                .bodyValue(bidRequest)
                .exchange()
                .expectStatus().isNoContent();
    }
}
