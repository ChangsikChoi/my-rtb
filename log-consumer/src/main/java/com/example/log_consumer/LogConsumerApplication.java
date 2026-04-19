package com.example.log_consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LogConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogConsumerApplication.class, args);
	}

}
