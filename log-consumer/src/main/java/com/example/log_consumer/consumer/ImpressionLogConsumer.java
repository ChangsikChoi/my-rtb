package com.example.log_consumer.consumer;

import com.example.KafkaImpressionLog;
import com.example.log_consumer.model.ImpressionLog;
import com.example.log_consumer.repository.ImpressionLogRepository;
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
public class ImpressionLogConsumer {
    private final ImpressionLogRepository impressionLogRepository;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(topics = "impression-log", groupId = "impression-log-group")
    public void consume(KafkaImpressionLog message) {
        ImpressionLog log = new ImpressionLog(
                null,
                message.getRequestId(),
                message.getCampaignId(),
                message.getCreativeId());

        System.out.println("message = " + message);
        ImpressionLog save = impressionLogRepository.save(log);
        System.out.println("save = " + save);
    }

    @DltHandler
    public void dltConsume(KafkaImpressionLog message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.out.println("DLT message = " + message);
    }
}
