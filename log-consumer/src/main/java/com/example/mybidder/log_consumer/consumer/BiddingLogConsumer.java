package com.example.mybidder.log_consumer.consumer;

import com.example.mybidder.log_consumer.model.BiddingLog;
import com.example.mybidder.log_consumer.model.KafkaBidLog;
import com.example.mybidder.log_consumer.repository.BiddingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BiddingLogConsumer {
    private final BiddingLogRepository biddingLogRepository;

    @KafkaListener(topics = "bidding-log", groupId = "bidding-log-group")
    public void consume(KafkaBidLog message) {
        BiddingLog log = new BiddingLog(
                null,
                message.requestId(),
                message.campaignId(),
                message.price());

        System.out.println("message = " + message);
        BiddingLog save = biddingLogRepository.save(log);
        System.out.println("save = " + save);
    }
}
