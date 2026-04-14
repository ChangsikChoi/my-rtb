package com.example.bidder.application.support;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuctionIdGenerator {

  public String generate() {
    return UUID.randomUUID().toString();
  }
}
