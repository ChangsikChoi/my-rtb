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
        WinLog log = new WinLog(
                null,
                message.getRequestId(),
                message.getCampaignId(),
                message.getCreativeId());

        System.out.println("message = " + message);
        WinLog save = winLogRepository.save(log);
        System.out.println("save = " + save);
    }
}
