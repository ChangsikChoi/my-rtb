package com.example.bidder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class BiddingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiddingApplication.class, args);
        Hooks.enableAutomaticContextPropagation();
    }
}
