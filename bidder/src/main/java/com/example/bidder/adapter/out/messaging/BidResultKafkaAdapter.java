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
    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setAuctionId(bidResult.auctionId())
        .setRequestId(bidResult.requestId())
        .setCampaignId(bidResult.campaignId())
        .setCreativeId(bidResult.creativeId())
        .setPriceMicro(bidResult.impressionPriceMicro())
        .setReceivedAt(bidResult.receivedAt())
        .build();

    CompletableFuture<SendResult<String, SpecificRecordBase>> future =
        kafkaTemplate.send(topicName, bidResult.auctionId(), message);

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
