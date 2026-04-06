package com.sporty.bettask.integration;

import com.sporty.bettask.dto.EventOutcomeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldExposeActuatorHealthAndPrometheusEndpoints() {
        ResponseEntity<String> healthResponse = testSupport.get("/actuator/health");
        ResponseEntity<String> prometheusResponse = testSupport.get("/actuator/prometheus");

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("UP");
        assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prometheusResponse.getBody()).contains("jvm_");
    }

    @Test
    void shouldExposeCustomMicrometerCountersAfterProcessingAnEvent() {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-6006", "Team C vs Team D", "TEAM-A");
        ResponseEntity<String> response = testSupport.postJson("/api/event-outcomes", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        testSupport.awaitAsserted(() -> {
            ResponseEntity<String> prometheusResponse = testSupport.get("/actuator/prometheus");

            assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(prometheusResponse.getBody()).contains("bet_task_event_outcomes_published_total");
            assertThat(prometheusResponse.getBody()).contains("bet_task_settlements_emitted_total");
        });
    }
}
