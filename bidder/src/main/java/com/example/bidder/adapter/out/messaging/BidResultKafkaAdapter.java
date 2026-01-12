package com.example.bidder.adapter.out.messaging;

import com.example.KafkaBiddingLog;
import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.port.out.SendBidResultPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidResultKafkaAdapter implements SendBidResultPort {

  private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
  private static final String topicName = "bidding-log";

  @Override
  public void sendBidResult(Bid bidResult) {
    // cpm 단위 가격을 단일 가격으로 변환
    long bidPriceMicro = bidResult.bidPriceCpmMicro() / 1000;

    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setRequestId(bidResult.requestId())
        .setCampaignId(bidResult.campaignId())
        .setCreativeId(bidResult.creativeId())
        .setPriceMicro(bidPriceMicro)
        .build();

    CompletableFuture<SendResult<String, SpecificRecordBase>> future =
        kafkaTemplate.send(topicName, bidResult.requestId(), message);

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
