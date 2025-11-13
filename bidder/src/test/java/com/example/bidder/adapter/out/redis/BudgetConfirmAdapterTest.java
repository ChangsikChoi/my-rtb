package com.example.bidder.adapter.out.redis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class BudgetConfirmAdapterTest {

  @Container
  @ServiceConnection
  static GenericContainer<?> redis = new GenericContainer<>("redis:7")
      .withExposedPorts(6379);

  @Autowired
  private ReactiveStringRedisTemplate redisTemplate;
  @Autowired
  private BudgetConfirmAdapter budgetConfirmAdapter;

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
  }


}