package com.sporty.bettask.messaging;

import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.service.BetSettlementService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventOutcomeConsumer {

    private final BetSettlementService betSettlementService;
    private final Counter processedCounter;
    private final Counter failedCounter;

    public EventOutcomeConsumer(BetSettlementService betSettlementService, MeterRegistry meterRegistry) {
        this.betSettlementService = betSettlementService;
        this.processedCounter = Counter.builder("bet_task_event_outcomes_consumed_total")
                .description("Total number of event outcomes consumed successfully")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("bet_task_event_outcomes_failed_total")
                .description("Total number of event outcome consumptions that failed")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "${app.kafka.event-outcomes-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(EventOutcomeMessage message) {
        MDC.put("eventId", message.eventId());
        try {
            log.info("Consumed event outcome winnerId={}", message.eventWinnerId());
            betSettlementService.settle(message);
            processedCounter.increment();
        } catch (ObjectOptimisticLockingFailureException exception) {
            log.info("Skipping already-settled event outcome (optimistic lock conflict)", exception);
            processedCounter.increment();
        } catch (RuntimeException exception) {
            failedCounter.increment();
            log.error("Failed to process event outcome", exception);
            throw exception;
        } finally {
            MDC.remove("eventId");
        }
    }
}
