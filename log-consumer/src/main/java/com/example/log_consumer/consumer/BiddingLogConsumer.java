package com.example.log_consumer.consumer;

import com.example.KafkaBiddingLog;
import com.example.log_consumer.model.BiddingLog;
import com.example.log_consumer.repository.BiddingLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BiddingLogConsumer {
    private static final Logger dltLogger = LoggerFactory.getLogger("dlt.bidding-log");

    private final BiddingLogRepository biddingLogRepository;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(topics = "bidding-log", groupId = "bidding-log-group")
    public void consume(KafkaBiddingLog message) {
        BiddingLog log = BiddingLog.builder()
                .auctionId(message.getAuctionId())
                .requestId(message.getRequestId())
                .campaignId(message.getCampaignId())
                .creativeId(message.getCreativeId())
                .priceMicro(message.getPriceMicro())
                .receivedAt(message.getReceivedAt())
                .build();

        biddingLogRepository.save(log);
    }

    @DltHandler
    public void dltConsume(KafkaBiddingLog message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        dltLogger.warn("DLT topic={}, message={}", topic, message);
    }
}
