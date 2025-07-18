package com.example.mybidder.log_consumer.consumer;

import com.example.KafkaBiddingLog;
import com.example.mybidder.log_consumer.model.BiddingLog;
import com.example.mybidder.log_consumer.repository.BiddingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BiddingLogConsumer {
    private final BiddingLogRepository biddingLogRepository;

    @KafkaListener(topics = "bidding-log", groupId = "bidding-log-group")
    public void consume(KafkaBiddingLog message) {
        BiddingLog log = new BiddingLog(
                null,
                message.getRequestId(),
                message.getCampaignId(),
                message.getPrice());

        System.out.println("message = " + message);
        BiddingLog save = biddingLogRepository.save(log);
        System.out.println("save = " + save);
    }
}
