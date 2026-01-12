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
        ClickLog log = new ClickLog(
                null,
                message.getRequestId(),
                message.getCampaignId(),
                message.getCreativeId());

        System.out.println("message = " + message);
        ClickLog save = clickLogRepository.save(log);
        System.out.println("save = " + save);
    }
}
