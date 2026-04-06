package com.sporty.bettask.domain;

import java.math.BigDecimal;

public record BetSettlementMessage(
        String betId,
        String userId,
        String eventId,
        String outcomeWinnerId,
        String selectedWinnerId,
        BigDecimal betAmount,
        SettlementResult result
) {
}

