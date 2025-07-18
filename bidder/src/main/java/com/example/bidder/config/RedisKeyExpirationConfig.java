package com.example.bidder.config;

import com.example.bidder.adapter.out.redis.RedisExpiredKeyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisKeyExpirationConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public KeyExpirationEventMessageListener keyExpirationEventMessageListener(
            RedisMessageListenerContainer container,
            RedisExpiredKeyHandler redisExpiredKeyHandler) {

        return new KeyExpirationEventMessageListener(container) {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                String expiredKey = message.toString();
                redisExpiredKeyHandler.handleExpiredKey(expiredKey);
            }
        };
    }
}
