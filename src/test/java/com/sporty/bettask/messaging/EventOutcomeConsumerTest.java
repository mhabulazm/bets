package com.sporty.bettask.messaging;

import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.service.BetSettlementService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class EventOutcomeConsumerTest {

    @Mock
    private BetSettlementService betSettlementService;

    private EventOutcomeConsumer eventOutcomeConsumer;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        eventOutcomeConsumer = new EventOutcomeConsumer(betSettlementService, meterRegistry);
    }

    @Test
    void shouldDelegateConsumedMessageToSettlementService() {
        EventOutcomeMessage message = new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A");

        eventOutcomeConsumer.consume(message);

        verify(betSettlementService).settle(message);
        assertThat(meterRegistry.get("bet_task_event_outcomes_consumed_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("bet_task_event_outcomes_failed_total").counter().count()).isEqualTo(0.0);
    }

    @Test
    void shouldIncrementFailedCounterAndRethrowWhenSettlementFails() {
        EventOutcomeMessage message = new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A");
        doThrow(new IllegalStateException("boom")).when(betSettlementService).settle(message);

        assertThatThrownBy(() -> eventOutcomeConsumer.consume(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(meterRegistry.get("bet_task_event_outcomes_consumed_total").counter().count()).isEqualTo(0.0);
        assertThat(meterRegistry.get("bet_task_event_outcomes_failed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void shouldSwallowOptimisticLockingFailureAndCountAsProcessed() {
        EventOutcomeMessage message = new EventOutcomeMessage("EVT-1001", "Team A vs Team B", "TEAM-A");
        doThrow(new ObjectOptimisticLockingFailureException("Bet", "BET-1"))
                .when(betSettlementService).settle(message);

        assertThatCode(() -> eventOutcomeConsumer.consume(message)).doesNotThrowAnyException();

        assertThat(meterRegistry.get("bet_task_event_outcomes_consumed_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("bet_task_event_outcomes_failed_total").counter().count()).isEqualTo(0.0);
    }
}
