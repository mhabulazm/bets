package com.sporty.bettask.mapper;

import com.sporty.bettask.domain.BetSettlementMessage;
import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.domain.SettlementResult;
import com.sporty.bettask.entity.Bet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BetSettlementMapper {

    @Mapping(source = "outcome.eventWinnerId", target = "outcomeWinnerId")
    @Mapping(source = "bet.selectedWinnerId", target = "selectedWinnerId")
    @Mapping(source = "bet.betAmount", target = "betAmount")
    @Mapping(source = "bet.betId", target = "betId")
    @Mapping(source = "bet.userId", target = "userId")
    @Mapping(source = "bet.eventId", target = "eventId")
    BetSettlementMessage toMessage(Bet bet, EventOutcomeMessage outcome, SettlementResult result);
}
