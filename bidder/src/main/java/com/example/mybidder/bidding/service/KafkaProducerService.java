package com.example.mybidder.bidding.service;

import com.example.mybidder.bidding.model.KafkaBidLog;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, KafkaBidLog> kafkaTemplate;

    public void sendBidLog(KafkaBidLog message) {
        CompletableFuture<SendResult<String, KafkaBidLog>> future = kafkaTemplate.send("bidding-log", message);
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
