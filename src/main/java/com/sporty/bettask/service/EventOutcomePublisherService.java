package com.sporty.bettask.service;

import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.dto.EventOutcomeRequest;
import com.sporty.bettask.exception.KafkaPublishException;
import com.sporty.bettask.mapper.EventOutcomeMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class EventOutcomePublisherService {

    private final KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate;
    private final String topic;
    private final long publishTimeoutSeconds;
    private final Counter publishCounter;
    private final EventOutcomeMapper eventOutcomeMapper;

    public EventOutcomePublisherService(
            KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate,
            @Value("${app.kafka.event-outcomes-topic}") String topic,
            @Value("${app.kafka.publish-timeout-seconds:5}") long publishTimeoutSeconds,
            MeterRegistry meterRegistry,
            EventOutcomeMapper eventOutcomeMapper
    ) {
        if (publishTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("publishTimeoutSeconds must be positive, got: " + publishTimeoutSeconds);
        }
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishTimeoutSeconds = publishTimeoutSeconds;
        this.publishCounter = Counter.builder("bet_task_event_outcomes_published_total")
                .description("Total number of event outcomes published to Kafka")
                .register(meterRegistry);
        this.eventOutcomeMapper = eventOutcomeMapper;
    }

    public void publish(EventOutcomeRequest request) {
        EventOutcomeMessage message = eventOutcomeMapper.toMessage(request);

        try {
            kafkaTemplate.send(topic, message.eventId(), message).get(publishTimeoutSeconds, TimeUnit.SECONDS);
            publishCounter.increment();
            log.info("Published event outcome topic={} eventId={}", topic, message.eventId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Interrupted while publishing event outcome", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaPublishException("Failed to publish event outcome to Kafka", exception);
        }
    }
}
