package com.example.mybidder.controller;

import com.example.mybidder.bidding.controller.BidController;
import com.example.mybidder.bidding.model.BidRequest;
import com.example.mybidder.bidding.model.BidResponse;
import com.example.mybidder.bidding.service.BidService;
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
        BidResponse bidResponse = new BidResponse(
                "test",
                new BigDecimal(100),
                "http://localhost:8080/dsp/win?rid=test",
                "<img src='http://localhost:8080/dsp/imp?rid=test />"
        );
        BidRequest bidRequest = new BidRequest("test", "seoul", new BigDecimal(99));
        when(bidService.bid(bidRequest)).thenReturn(Mono.just(bidResponse));

        webTestClient.post().uri("/dsp/bid")
                .bodyValue(bidRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void 입찰_실패() {
        BidRequest bidRequest = new BidRequest("test", "seoul", new BigDecimal(99));
        when(bidService.bid(bidRequest)).thenReturn(Mono.empty());

        webTestClient.post().uri("/dsp/bid")
                .bodyValue(bidRequest)
                .exchange()
                .expectStatus().isNoContent();
    }
}
