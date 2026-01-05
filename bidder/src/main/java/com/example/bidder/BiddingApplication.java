package com.example.bidder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class BiddingApplication {
    public static void main(String[] args) {
        BlockHound.install(builder ->
            builder.allowBlockingCallsInside("io.lettuce.core.RedisClient", "connect")
        );
        SpringApplication.run(BiddingApplication.class, args);
        Hooks.enableAutomaticContextPropagation();
    }
}
