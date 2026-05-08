package com.example.log_consumer.consumer;

import com.example.KafkaWinLog;
import com.example.log_consumer.model.WinLog;
import com.example.log_consumer.repository.WinLogRepository;
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
public class WinLogConsumer {
    private static final Logger dltLogger = LoggerFactory.getLogger("dlt.win-log");

    private final WinLogRepository winLogRepository;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(topics = "win-log", groupId = "win-log-group")
    public void consume(KafkaWinLog message) {
        WinLog log = WinLog.builder()
                .auctionId(message.getAuctionId())
                .requestId(message.getRequestId())
                .campaignId(message.getCampaignId())
                .creativeId(message.getCreativeId())
                .receivedAt(message.getReceivedAt())
                .build();

        WinLog save = winLogRepository.save(log);
    }

    @DltHandler
    public void dltConsume(KafkaWinLog message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        dltLogger.warn("DLT topic={}, message={}", topic, message);
    }
}
