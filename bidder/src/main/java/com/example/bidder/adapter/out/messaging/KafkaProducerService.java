package com.example.bidder.adapter.out.messaging;

import com.example.KafkaBiddingLog;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, KafkaBiddingLog> kafkaTemplate;

    public void sendBidLog(KafkaBiddingLog message) {
        CompletableFuture<SendResult<String, KafkaBiddingLog>> future = kafkaTemplate.send("bidding-log", message);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                System.out.println("Failed to send message: " + ex.getMessage());
                System.out.println(result.getProducerRecord().value());
            } else {
                System.out.println("Message sent successfully: " + result.getProducerRecord().value());
            }
        });
    }
}
