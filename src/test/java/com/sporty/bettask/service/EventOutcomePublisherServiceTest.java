package com.sporty.bettask.service;

import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.dto.EventOutcomeRequest;
import com.sporty.bettask.exception.KafkaPublishException;
import com.sporty.bettask.mapper.EventOutcomeMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventOutcomePublisherServiceTest {

    @Mock
    private KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate;

    private final EventOutcomeMapper eventOutcomeMapper = Mappers.getMapper(EventOutcomeMapper.class);

    private EventOutcomePublisherService eventOutcomePublisherService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        eventOutcomePublisherService = new EventOutcomePublisherService(kafkaTemplate, "event-outcomes", 5L, meterRegistry, eventOutcomeMapper);
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
        Thread.interrupted();
    }

    @Test
    void shouldPublishEventOutcomeAndIncrementCounter() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A");
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, EventOutcomeMessage>> future = mock(CompletableFuture.class);

        when(kafkaTemplate.send(eq("event-outcomes"), eq("EVT-1001"), any(EventOutcomeMessage.class)))
                .thenReturn(future);
        when(future.get(5L, TimeUnit.SECONDS)).thenReturn(null);

        eventOutcomePublisherService.publish(request);

        ArgumentCaptor<EventOutcomeMessage> captor = ArgumentCaptor.forClass(EventOutcomeMessage.class);
        verify(kafkaTemplate).send(eq("event-outcomes"), eq("EVT-1001"), captor.capture());
        assertThat(captor.getValue().eventName()).isEqualTo("Team A vs Team B");
        assertThat(meterRegistry.get("bet_task_event_outcomes_published_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void shouldWrapExecutionFailureInKafkaPublishException() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A");
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, EventOutcomeMessage>> future = mock(CompletableFuture.class);

        when(kafkaTemplate.send(eq("event-outcomes"), eq("EVT-1001"), any(EventOutcomeMessage.class)))
                .thenReturn(future);
        when(future.get(5L, TimeUnit.SECONDS))
                .thenThrow(new ExecutionException(new RuntimeException("broker unavailable")));

        assertThatThrownBy(() -> eventOutcomePublisherService.publish(request))
                .isInstanceOf(KafkaPublishException.class)
                .hasMessage("Failed to publish event outcome to Kafka");
    }

    @Test
    void shouldWrapTimeoutFailureInKafkaPublishException() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A");
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, EventOutcomeMessage>> future = mock(CompletableFuture.class);

        when(kafkaTemplate.send(eq("event-outcomes"), eq("EVT-1001"), any(EventOutcomeMessage.class)))
                .thenReturn(future);
        when(future.get(5L, TimeUnit.SECONDS))
                .thenThrow(new TimeoutException("timeout"));

        assertThatThrownBy(() -> eventOutcomePublisherService.publish(request))
                .isInstanceOf(KafkaPublishException.class)
                .hasMessage("Failed to publish event outcome to Kafka");
    }

    @Test
    void shouldRestoreInterruptedStatusWhenPublishIsInterrupted() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A");
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, EventOutcomeMessage>> future = mock(CompletableFuture.class);

        when(kafkaTemplate.send(eq("event-outcomes"), eq("EVT-1001"), any(EventOutcomeMessage.class)))
                .thenReturn(future);
        when(future.get(5L, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("interrupted"));

        assertThatThrownBy(() -> eventOutcomePublisherService.publish(request))
                .isInstanceOf(KafkaPublishException.class)
                .hasMessage("Interrupted while publishing event outcome");

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
