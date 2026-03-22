package com.example.bidder.config;

import com.example.bidder.adapter.out.redis.RedisExpiredKeyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "bidder.redis.key-expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RedisKeyExpirationConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        log.info("Redis key expiration listener container created");
        return container;
    }

    @Bean
    public KeyExpirationEventMessageListener keyExpirationEventMessageListener(
            RedisMessageListenerContainer container,
            RedisExpiredKeyHandler redisExpiredKeyHandler) {
        log.info("Redis key expiration event listener created");

        return new KeyExpirationEventMessageListener(container) {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                String expiredKey = message.toString();
                redisExpiredKeyHandler.handleExpiredKey(expiredKey);
            }
        };
    }
}
