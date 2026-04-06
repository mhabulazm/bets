package com.sporty.bettask.controller;

import tools.jackson.databind.ObjectMapper;
import com.sporty.bettask.dto.EventOutcomeRequest;
import com.sporty.bettask.exception.KafkaPublishException;
import com.sporty.bettask.service.EventOutcomePublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventOutcomeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private EventOutcomePublisherService eventOutcomePublisherService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        eventOutcomePublisherService = Mockito.mock(EventOutcomePublisherService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new EventOutcomeController(eventOutcomePublisherService, "event-outcomes"))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldAcceptValidRequest() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A");

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.topic").value("event-outcomes"))
                .andExpect(jsonPath("$.eventId").value("EVT-1001"));

        verify(eventOutcomePublisherService).publish(request);
    }

    @Test
    void shouldRejectBlankEventId() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("", "Team A vs Team B", "TEAM-A");

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("eventId"));
    }

    @Test
    void shouldRejectBlankEventName() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "", "TEAM-A");

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("eventName"));
    }

    @Test
    void shouldRejectBlankEventWinnerId() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "");

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("eventWinnerId"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenKafkaPublishFails() throws Exception {
        EventOutcomeRequest request = new EventOutcomeRequest("EVT-1001", "Team A vs Team B", "TEAM-A");
        doThrow(new KafkaPublishException("Failed to publish event outcome to Kafka", new RuntimeException("boom")))
                .when(eventOutcomePublisherService)
                .publish(request);

        mockMvc.perform(post("/api/event-outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("KAFKA_PUBLISH_ERROR"))
                .andExpect(jsonPath("$.message").value("Failed to publish event outcome to Kafka"));
    }
}
