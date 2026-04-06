package com.sporty.bettask.integration;

import com.sporty.bettask.domain.BetSettlementMessage;
import com.sporty.bettask.domain.SettlementResult;
import com.sporty.bettask.dto.EventOutcomeRequest;
import com.sporty.bettask.messaging.RocketMqProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class EventOutcomeFlowIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    private RocketMqProducer rocketMqProducer;

    @BeforeEach
    void setUp() {
        reset(rocketMqProducer);
    }

    @Test
    void shouldPublishConsumeAndEmitSettlementMessages() {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-3003", "Team A vs Team B", "TEAM-A");
        ResponseEntity<String> response = testSupport.postJson("/api/event-outcomes", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        testSupport.awaitAsserted(() -> verify(rocketMqProducer, times(2)).sendSettlement(any()));
    }

    @Test
    void shouldEmitWonAndLostSettlementPayloads() {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-4004", "Team A vs Team B", "TEAM-A");
        ResponseEntity<String> response = testSupport.postJson("/api/event-outcomes", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        org.mockito.ArgumentCaptor<BetSettlementMessage> captor =
                org.mockito.ArgumentCaptor.forClass(BetSettlementMessage.class);

        verify(rocketMqProducer, timeout(10_000).times(2)).sendSettlement(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(BetSettlementMessage::result)
                .containsExactlyInAnyOrder(
                        SettlementResult.WON,
                        SettlementResult.LOST
                );
    }

    @Test
    void shouldNotSettleSameBetsTwiceOnDuplicateEvent() {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-5005", "Team A vs Team B", "TEAM-A");

        testSupport.postJson("/api/event-outcomes", request);
        testSupport.awaitAsserted(() -> verify(rocketMqProducer, times(2)).sendSettlement(any()));

        reset(rocketMqProducer);

        testSupport.postJson("/api/event-outcomes", request);
        verify(rocketMqProducer, after(3_000).never()).sendSettlement(any());
    }
}
