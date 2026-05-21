package com.example.log_consumer.consumer;

import com.example.KafkaBiddingLog;
import com.example.log_consumer.model.BiddingLog;
import com.example.log_consumer.repository.BiddingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BiddingLogConsumer {

    private final BiddingLogRepository biddingLogRepository;

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
}
