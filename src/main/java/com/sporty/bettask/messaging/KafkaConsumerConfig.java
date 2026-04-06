package com.sporty.bettask.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    // TODO: Consider ExponentialBackOff for production resilience against transient broker/network issues.
    //       Current config: 3 total attempts (1 initial + 2 retries) with 1s fixed delay.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                (consumerRecord, exception) -> log.error(
                        "Exhausted retries for consumerRecord topic={} partition={} offset={}",
                        consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), exception
                ),
                new FixedBackOff(1_000L, 2)
        );
    }
}
