package com.example.log_consumer.consumer;

import com.example.KafkaClickLog;
import com.example.log_consumer.model.ClickLog;
import com.example.log_consumer.repository.ClickLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClickLogConsumer {

    private final ClickLogRepository clickLogRepository;

    @KafkaListener(topics = "click-log", groupId = "click-log-group")
    public void consume(KafkaClickLog message) {
        ClickLog log = ClickLog.builder()
                .auctionId(message.getAuctionId())
                .requestId(message.getRequestId())
                .campaignId(message.getCampaignId())
                .creativeId(message.getCreativeId())
                .receivedAt(message.getReceivedAt())
                .build();

        clickLogRepository.save(log);
    }
}
