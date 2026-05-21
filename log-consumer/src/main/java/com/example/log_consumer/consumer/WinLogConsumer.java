package com.example.log_consumer.consumer;

import com.example.KafkaWinLog;
import com.example.log_consumer.model.WinLog;
import com.example.log_consumer.repository.WinLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WinLogConsumer {

    private final WinLogRepository winLogRepository;

    @KafkaListener(topics = "win-log", groupId = "win-log-group")
    public void consume(KafkaWinLog message) {
        WinLog log = WinLog.builder()
                .auctionId(message.getAuctionId())
                .requestId(message.getRequestId())
                .campaignId(message.getCampaignId())
                .creativeId(message.getCreativeId())
                .receivedAt(message.getReceivedAt())
                .build();

        winLogRepository.save(log);
    }
}
