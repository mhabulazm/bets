package com.sporty.bettask.domain;

public record EventOutcomeMessage(
        String eventId,
        String eventName,
        String eventWinnerId
) {
}

