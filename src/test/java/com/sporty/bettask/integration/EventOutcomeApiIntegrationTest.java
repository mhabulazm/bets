package com.sporty.bettask.integration;

import com.sporty.bettask.messaging.RocketMqProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

class EventOutcomeApiIntegrationTest extends AbstractIntegrationTest {

    @MockitoSpyBean
    private RocketMqProducer rocketMqProducer;

    @BeforeEach
    void setUp() {
        reset(rocketMqProducer);
    }

    @Test
    void shouldRejectInvalidPayloadWithBadRequest() {
        String invalidPayload = """
                {
                  "eventId": "",
                  "eventName": "Team A vs Team B",
                  "eventWinnerId": ""
                }
                """;

        ResponseEntity<String> response = testSupport.postRawJson("/api/event-outcomes", invalidPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
        verify(rocketMqProducer, never()).sendSettlement(org.mockito.ArgumentMatchers.any());
    }
}
