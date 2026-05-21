package com.example.log_consumer.consumer;

import com.example.KafkaImpressionLog;
import com.example.log_consumer.model.ImpressionLog;
import com.example.log_consumer.repository.ImpressionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImpressionLogConsumer {

    private final ImpressionLogRepository impressionLogRepository;

    @KafkaListener(topics = "impression-log", groupId = "impression-log-group")
    public void consume(KafkaImpressionLog message) {
        ImpressionLog log = ImpressionLog.builder()
                .auctionId(message.getAuctionId())
                .requestId(message.getRequestId())
                .campaignId(message.getCampaignId())
                .creativeId(message.getCreativeId())
                .receivedAt(message.getReceivedAt())
                .build();

        impressionLogRepository.save(log);
    }
}
