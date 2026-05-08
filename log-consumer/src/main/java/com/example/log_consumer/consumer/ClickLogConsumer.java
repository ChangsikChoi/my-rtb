package com.example.log_consumer.consumer;

import com.example.KafkaClickLog;
import com.example.log_consumer.model.ClickLog;
import com.example.log_consumer.repository.ClickLogRepository;
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
public class ClickLogConsumer {
    private static final Logger dltLogger = LoggerFactory.getLogger("dlt.click-log");

    private final ClickLogRepository clickLogRepository;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(topics = "click-log", groupId = "click-log-group")
    public void consume(KafkaClickLog message) {
        ClickLog log = ClickLog.builder()
                .auctionId(message.getAuctionId())
                .requestId(message.getRequestId())
                .campaignId(message.getCampaignId())
                .creativeId(message.getCreativeId())
                .receivedAt(message.getReceivedAt())
                .build();

        ClickLog save = clickLogRepository.save(log);
    }

    @DltHandler
    public void dltConsume(KafkaClickLog message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        dltLogger.warn("DLT topic={}, message={}", topic, message);
    }
}
