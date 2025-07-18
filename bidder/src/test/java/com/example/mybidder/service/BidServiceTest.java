package com.example.mybidder.service;

import com.example.bidder.application.BidRequestVo;
import com.example.bidder.adapter.out.redis.BudgetWithLuaScriptService;
import com.example.bidder.adapter.out.redis.Campaign;
import com.example.bidder.adapter.out.redis.RedisService;
import com.example.bidder.application.service.BidService;
import com.example.bidder.adapter.out.messaging.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BidServiceTest {

    private BudgetWithLuaScriptService budgetService;
    private RedisService redisService;
    private KafkaProducerService kafkaProducerService;
    private BidService bidService;

    @BeforeEach
    void setUp() {
        budgetService = mock(BudgetWithLuaScriptService.class);
        redisService = mock(RedisService.class);
        kafkaProducerService = mock(KafkaProducerService.class);
//        bidService = new BidService(budgetService, redisService, kafkaProducerService);
    }

    @Test
    void 입찰_성공() {
        BidRequestVo bidRequest = new BidRequestVo("test-1", "seoul", new BigDecimal(99));
        // 캠페인 cpm = 1원, micro 환산 시 1백만. -> 노출 당 비용 마이크로 값 = 1백만 / 1000 -> 예산 소진 값 1000
        Campaign campaign = new Campaign("test-campaign-1", "test", "seoul",
                1_000_000, 100_000_000, 1_000_000);

        when(redisService.getHighestCpmCampaign("seoul", new BigDecimal(99)))
                .thenReturn(Mono.just(campaign));

        when(budgetService.reserveBudget("test-campaign-1", "test-1", 1000L, Duration.ofSeconds(30)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(bidService.bid(bidRequest))
                .expectNextMatches(response -> {
                    assertThat(response.requestId()).isEqualTo("test-1");
                    assertThat(response.price().compareTo(new BigDecimal(1))).isZero();
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void 입찰_실패_캠페인없음() {
        BidRequestVo bidRequest = new BidRequestVo("test-1", "seoul", new BigDecimal(99));

        when(redisService.getHighestCpmCampaign("seoul", new BigDecimal(99)))
                .thenReturn(Mono.empty());

        StepVerifier.create(bidService.bid(bidRequest))
                .verifyComplete();
    }

    @Test
    void 입찰_실패_선점가능예산없음() {
        BidRequestVo bidRequest = new BidRequestVo("test-1", "seoul", new BigDecimal(99));
        Campaign campaign = new Campaign("test-campaign-1", "test", "seoul",
                1_000_000, 100_000_000, 1_000_000);

        when(redisService.getHighestCpmCampaign("seoul", new BigDecimal(99)))
                .thenReturn(Mono.just(campaign));

        when(budgetService.reserveBudget("test-campaign-1", "test-1", 1000L, Duration.ofSeconds(30)))
                .thenReturn(Mono.just(false));

        StepVerifier.create(bidService.bid(bidRequest))
                .verifyComplete();
    }


}