package com.example.bidder.adapter.out.messaging;

import com.example.KafkaClickLog;
import com.example.bidder.domain.model.Click;
import com.example.bidder.domain.port.out.SendClickPort;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickKafkaAdapter implements SendClickPort {

  private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

  @Override
  public void sendClick(Click click) {
    KafkaClickLog message = KafkaClickLog.newBuilder()
        .setRequestId(click.id())
        .setCampaignId(click.campaignId())
        .setCreativeId(click.creativeId())
        .build();

    CompletableFuture<SendResult<String, SpecificRecordBase>> future =
        kafkaTemplate.send("click-log", click.id(), message);

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        log.info("Failed to send message: {}", ex.getMessage());
        log.info(result.getProducerRecord().value().toString());
      } else {
        log.info("Message sent successfully: {}", result.getProducerRecord().value());
        log.info(result.toString());
      }
    });
  }
}
