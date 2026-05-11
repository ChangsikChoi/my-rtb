package com.example.log_consumer;

import com.example.log_consumer.support.PostgresTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LogConsumerApplicationTests extends PostgresTestContainerSupport {

	@Test
	void contextLoads() {
	}

}
