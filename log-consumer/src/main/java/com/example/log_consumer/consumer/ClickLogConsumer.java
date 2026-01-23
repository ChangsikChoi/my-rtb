package com.example.log_consumer.consumer;

import com.example.KafkaClickLog;
import com.example.log_consumer.model.ClickLog;
import com.example.log_consumer.repository.ClickLogRepository;
import lombok.RequiredArgsConstructor;
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
    private final ClickLogRepository clickLogRepository;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(topics = "click-log", groupId = "click-log-group")
    public void consume(KafkaClickLog message) {
        ClickLog log = new ClickLog(
                null,
                message.getRequestId(),
                message.getCampaignId(),
                message.getCreativeId());

        System.out.println("message = " + message);
        ClickLog save = clickLogRepository.save(log);
        System.out.println("save = " + save);
    }

    @DltHandler
    public void dltConsume(KafkaClickLog message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.out.println("DLT message = " + message);
    }
}
