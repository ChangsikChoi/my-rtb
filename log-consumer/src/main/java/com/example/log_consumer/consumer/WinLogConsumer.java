package com.example.log_consumer.consumer;

import com.example.KafkaWinLog;
import com.example.log_consumer.model.WinLog;
import com.example.log_consumer.repository.WinLogRepository;
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
public class WinLogConsumer {
    private final WinLogRepository winLogRepository;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @KafkaListener(topics = "win-log", groupId = "win-log-group")
    public void consume(KafkaWinLog message) {
        WinLog log = new WinLog(
                null,
                message.getRequestId(),
                message.getCampaignId(),
                message.getCreativeId());

        System.out.println("message = " + message);
        WinLog save = winLogRepository.save(log);
        System.out.println("save = " + save);
    }

    @DltHandler
    public void dltConsume(KafkaWinLog message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.out.println("DLT message = " + message);
    }
}
