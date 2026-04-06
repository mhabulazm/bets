package com.sporty.bettask.service;

import com.sporty.bettask.domain.BetSettlementMessage;
import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.domain.SettlementResult;
import com.sporty.bettask.entity.Bet;
import com.sporty.bettask.mapper.BetSettlementMapper;
import com.sporty.bettask.messaging.RocketMqProducer;
import com.sporty.bettask.repository.BetRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BetSettlementService {

    private final BetRepository betRepository;
    private final RocketMqProducer rocketMqProducer;
    private final Counter settlementCounter;
    private final BetSettlementMapper betSettlementMapper;

    public BetSettlementService(BetRepository betRepository, RocketMqProducer rocketMqProducer, MeterRegistry meterRegistry, BetSettlementMapper betSettlementMapper) {
        this.betRepository = betRepository;
        this.rocketMqProducer = rocketMqProducer;
        this.settlementCounter = Counter.builder("bet_task_settlements_emitted_total")
                .description("Total number of bet settlement messages emitted")
                .register(meterRegistry);
        this.betSettlementMapper = betSettlementMapper;
    }

    @Transactional
    public void settle(EventOutcomeMessage outcome) {
        List<Bet> bets = betRepository.findByEventIdAndSettledFalse(outcome.eventId());

        if (bets.isEmpty()) {
            log.warn("No unsettled bets found for eventId={}", outcome.eventId());
            return;
        }

        log.info("Found {} unsettled bets for eventId={}", bets.size(), outcome.eventId());

        List<BetSettlementMessage> messages = new ArrayList<>(bets.size());

        for (Bet bet : bets) {
            BetSettlementMessage settlementMessage = betSettlementMapper.toMessage(
                    bet, outcome, resolveResult(bet, outcome)
            );
            messages.add(settlementMessage);
            bet.markSettled();
            log.debug("Settled betId={} result={}", bet.getBetId(), settlementMessage.result());
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (BetSettlementMessage message : messages) {
                    rocketMqProducer.sendSettlement(message);
                    settlementCounter.increment();
                }
            }
        });
    }

    private SettlementResult resolveResult(Bet bet, EventOutcomeMessage outcome) {
        return bet.getSelectedWinnerId().equals(outcome.eventWinnerId())
                ? SettlementResult.WON
                : SettlementResult.LOST;
    }
}
