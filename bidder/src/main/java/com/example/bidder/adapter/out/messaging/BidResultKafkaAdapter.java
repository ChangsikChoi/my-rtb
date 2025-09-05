package com.example.bidder.adapter.out.messaging;

import com.example.KafkaBiddingLog;
import com.example.bidder.domain.model.Bid;
import com.example.bidder.domain.port.out.SendBidResultPort;
import com.example.bidder.utils.MicroConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class BidResultKafkaAdapter implements SendBidResultPort {

  private final KafkaTemplate<String, KafkaBiddingLog> kafkaTemplate;

  @Override
  public void sendBidResult(Bid bidResult) {
    // cpm 단위 가격을 단일 가격으로 변환
    long bidPriceMicro = bidResult.bidPriceCpmMicro() / 1000;
    double bidPrice = MicroConverter.fromMirco(bidPriceMicro).doubleValue();

    KafkaBiddingLog message = KafkaBiddingLog.newBuilder()
        .setRequestId(bidResult.requestId())
        .setCampaignId(bidResult.campaignId())
        .setCreativeId(bidResult.creativeId())
        .setPrice(bidPrice)
        .build();

    CompletableFuture<SendResult<String, KafkaBiddingLog>> future =
        kafkaTemplate.send("bidding-log", message);

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        System.out.println("Failed to send message: " + ex.getMessage());
        System.out.println(result.getProducerRecord().value());
      } else {
        System.out.println("Message sent successfully: " + result.getProducerRecord().value());
      }
    });
  }
}
