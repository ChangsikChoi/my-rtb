package com.example.bidder.adapter.out.messaging;

import com.example.KafkaWinLog;
import com.example.bidder.domain.model.Win;
import com.example.bidder.domain.port.out.SendWinResultPort;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WinResultKafkaAdapter implements SendWinResultPort {

  private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
  private static final String topicName = "win-log";

  @Override
  public void sendWinResult(Win winResult) {
    KafkaWinLog message = KafkaWinLog.newBuilder()
        .setAuctionId(winResult.auctionId())
        .setRequestId(winResult.requestId())
        .setCampaignId(winResult.campaignId())
        .setCreativeId(winResult.creativeId())
        .setReceivedAt(winResult.receivedAt())
        .build();

    CompletableFuture<SendResult<String, SpecificRecordBase>> future =
        kafkaTemplate.send(topicName, winResult.auctionId(), message);

    future.whenComplete((result, ex) -> {
      SpecificRecordBase logMessage = result != null && result.getProducerRecord() != null
          ? result.getProducerRecord().value()
          : message;
      if (ex != null) {
        MDC.put("topicName", topicName);
        log.info("Failed to send message:{}", ex.getMessage());
        log.warn(logMessage.toString());
        MDC.remove("topicName");
      } else {
        log.info("Message sent successfully: {}", logMessage);
      }
    });
  }
}
