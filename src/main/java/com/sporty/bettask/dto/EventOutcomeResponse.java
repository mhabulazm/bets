package com.sporty.bettask.dto;

public record EventOutcomeResponse(
        String status,
        String topic,
        String eventId
) {
}

