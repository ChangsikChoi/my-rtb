package com.example.log_consumer.config;

import com.example.log_consumer.consumer.KafkaDeserializationFailureLogger;
import java.util.List;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.DestinationTopicResolver;
import org.springframework.kafka.retrytopic.RetryTopicComponentFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.lang.NonNull;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

@Configuration
public class KafkaRetryTopicConfig {

  private static final List<String> LOG_TOPICS = List.of(
      "bidding-log",
      "impression-log",
      "click-log",
      "win-log"
  );

  private final int retryAttempts;
  private final long retryDelayMs;
  private final double retryMultiplier;

  public KafkaRetryTopicConfig(
      @Value("${log-consumer.kafka.retry.attempts}") int retryAttempts,
      @Value("${log-consumer.kafka.retry.delay-ms}") long retryDelayMs,
      @Value("${log-consumer.kafka.retry.multiplier}") double retryMultiplier
  ) {
    if (retryAttempts < 1) {
      throw new IllegalArgumentException("log-consumer.kafka.retry.attempts must be greater than 0");
    }
    if (retryDelayMs < 1) {
      throw new IllegalArgumentException("log-consumer.kafka.retry.delay-ms must be greater than 0");
    }
    if (retryMultiplier < 1) {
      throw new IllegalArgumentException("log-consumer.kafka.retry.multiplier must be greater than or equal to 1");
    }

    this.retryAttempts = retryAttempts;
    this.retryDelayMs = retryDelayMs;
    this.retryMultiplier = retryMultiplier;
  }

  @Bean
  public RetryTopicConfiguration logRetryTopicConfiguration(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate) {
    RetryTopicConfigurationBuilder builder = RetryTopicConfigurationBuilder
        .newInstance()
        .includeTopics(LOG_TOPICS)
        .retryTopicSuffix("-retry")
        .dltSuffix("-dlt")
        .maxAttempts(retryAttempts)
        .dltHandlerMethod("dltLogHandler", "handle");

    if (retryMultiplier > 1) {
      return builder
          .exponentialBackoff(
              retryDelayMs,
              retryMultiplier,
              maxIntervalFor(retryDelayMs)
          )
          .create(kafkaTemplate);
    }

    return builder
        .useSingleTopicForSameIntervals()
        .fixedBackOff(retryDelayMs)
        .create(kafkaTemplate);
  }

  private long maxIntervalFor(long delayMs) {
    return Math.max(ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL, delayMs + 1);
  }

  @Bean
  public RetryTopicComponentFactory retryTopicComponentFactory(
      KafkaDeserializationFailureLogger deserializationFailureLogger
  ) {
    return new RetryTopicComponentFactory() {
      @Override
      @NonNull
      public DeadLetterPublishingRecovererFactory deadLetterPublishingRecovererFactory(
          @NonNull DestinationTopicResolver destinationTopicResolver
      ) {
        DeadLetterPublishingRecovererFactory factory = super.deadLetterPublishingRecovererFactory(
            destinationTopicResolver
        );
        factory.setDeadLetterPublisherCreator((templateResolver, destinationResolver) ->
            new DeadLetterPublishingRecoverer(templateResolver, (record, exception) -> {
              if (isDeserializationException(exception)) {
                deserializationFailureLogger.log(record, exception);
                return null;
              }
              return destinationResolver.apply(record, exception);
            })
        );
        return factory;
      }
    };
  }

  private static boolean isDeserializationException(Exception exception) {
    Throwable current = exception;
    while (current != null) {
      if (current instanceof DeserializationException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
