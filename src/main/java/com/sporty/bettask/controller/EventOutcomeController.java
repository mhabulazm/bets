package com.sporty.bettask.controller;

import com.sporty.bettask.dto.EventOutcomeRequest;
import com.sporty.bettask.dto.EventOutcomeResponse;
import com.sporty.bettask.service.EventOutcomePublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-outcomes")
@RequiredArgsConstructor
public class EventOutcomeController {

    private final EventOutcomePublisherService eventOutcomePublisherService;

    @Value("${app.kafka.event-outcomes-topic}")
    private final String topic;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventOutcomeResponse publish(@Valid @RequestBody EventOutcomeRequest request) {
        MDC.put("eventId", request.eventId());
        try {
            eventOutcomePublisherService.publish(request);
            return new EventOutcomeResponse("accepted", topic, request.eventId());
        } finally {
            MDC.remove("eventId");
        }
    }
}
