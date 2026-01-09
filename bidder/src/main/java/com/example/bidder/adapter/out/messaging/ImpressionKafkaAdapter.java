package com.example.bidder.adapter.out.messaging;

import com.example.KafkaImpressionLog;
import com.example.bidder.domain.model.Impression;
import com.example.bidder.domain.port.out.SendImpressionPort;
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
public class ImpressionKafkaAdapter implements SendImpressionPort {

  private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
  private static final String topicName = "impression-log";

  @Override
  public void sendImpression(Impression impression) {
    KafkaImpressionLog message = KafkaImpressionLog.newBuilder()
        .setRequestId(impression.id())
        .setCampaignId(impression.campaignId())
        .setCreativeId(impression.creativeId())
        .build();

    CompletableFuture<SendResult<String, SpecificRecordBase>> future =
        kafkaTemplate.send(topicName, impression.id(), message);

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        MDC.put("topicName", topicName);
        log.info("Failed to send message:{}", ex.getMessage());
        log.warn(result.getProducerRecord().value().toString());
        MDC.remove("topicName");
      } else {
        log.info("Message sent successfully: {}", result.getProducerRecord().value());
      }
    });
  }
}
