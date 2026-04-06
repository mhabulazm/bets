package com.sporty.bettask.service;

import com.sporty.bettask.domain.BetSettlementMessage;
import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.domain.SettlementResult;
import com.sporty.bettask.entity.Bet;
import com.sporty.bettask.mapper.BetSettlementMapper;
import com.sporty.bettask.messaging.RocketMqProducer;
import com.sporty.bettask.repository.BetRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BetSettlementServiceTest {

    @Mock
    private BetRepository betRepository;

    @Mock
    private RocketMqProducer rocketMqProducer;

    private final BetSettlementMapper betSettlementMapper = Mappers.getMapper(BetSettlementMapper.class);

    private BetSettlementService betSettlementService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        betSettlementService = new BetSettlementService(betRepository, rocketMqProducer, meterRegistry, betSettlementMapper);
        TransactionSynchronizationManager.initSynchronization();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void simulateTransactionCommit() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void shouldCreateCorrectSettlementMessagesWithAllFieldsMapped() {
        Bet winningBet = new Bet("BET-1", "USR-1", "EVT-1", "MKT-1", "TEAM-A", new BigDecimal("12.50"));
        Bet losingBet = new Bet("BET-2", "USR-2", "EVT-1", "MKT-1", "TEAM-B", new BigDecimal("15.00"));
        when(betRepository.findByEventIdAndSettledFalse("EVT-1")).thenReturn(List.of(winningBet, losingBet));

        betSettlementService.settle(new EventOutcomeMessage("EVT-1", "Fixture", "TEAM-A"));
        simulateTransactionCommit();

        ArgumentCaptor<BetSettlementMessage> captor = ArgumentCaptor.forClass(BetSettlementMessage.class);
        verify(rocketMqProducer, times(2)).sendSettlement(captor.capture());

        BetSettlementMessage won = captor.getAllValues().get(0);
        assertThat(won.betId()).isEqualTo("BET-1");
        assertThat(won.userId()).isEqualTo("USR-1");
        assertThat(won.eventId()).isEqualTo("EVT-1");
        assertThat(won.outcomeWinnerId()).isEqualTo("TEAM-A");
        assertThat(won.selectedWinnerId()).isEqualTo("TEAM-A");
        assertThat(won.betAmount()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(won.result()).isEqualTo(SettlementResult.WON);

        BetSettlementMessage lost = captor.getAllValues().get(1);
        assertThat(lost.betId()).isEqualTo("BET-2");
        assertThat(lost.userId()).isEqualTo("USR-2");
        assertThat(lost.eventId()).isEqualTo("EVT-1");
        assertThat(lost.outcomeWinnerId()).isEqualTo("TEAM-A");
        assertThat(lost.selectedWinnerId()).isEqualTo("TEAM-B");
        assertThat(lost.betAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(lost.result()).isEqualTo(SettlementResult.LOST);

        assertThat(winningBet.isSettled()).isTrue();
        assertThat(losingBet.isSettled()).isTrue();

        assertThat(meterRegistry.get("bet_task_settlements_emitted_total").counter().count()).isEqualTo(2.0);
    }

    @Test
    void shouldDoNothingWhenNoBetsMatch() {
        when(betRepository.findByEventIdAndSettledFalse("EVT-404")).thenReturn(List.of());

        betSettlementService.settle(new EventOutcomeMessage("EVT-404", "Fixture", "TEAM-A"));

        verify(rocketMqProducer, never()).sendSettlement(org.mockito.ArgumentMatchers.any());
        assertThat(meterRegistry.get("bet_task_settlements_emitted_total").counter().count()).isEqualTo(0.0);
    }

    @Test
    void shouldThrowNullPointerWhenSelectedWinnerIdIsNull() {
        Bet bet = new Bet("BET-1", "USR-1", "EVT-1", "MKT-1", null, new BigDecimal("10.00"));
        when(betRepository.findByEventIdAndSettledFalse("EVT-1")).thenReturn(List.of(bet));

        assertThatThrownBy(() -> betSettlementService.settle(new EventOutcomeMessage("EVT-1", "Fixture", "TEAM-A")))
                .isInstanceOf(NullPointerException.class);

        verify(rocketMqProducer, never()).sendSettlement(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldTreatNullEventWinnerIdAsLost() {
        Bet bet = new Bet("BET-1", "USR-1", "EVT-1", "MKT-1", "TEAM-A", new BigDecimal("10.00"));
        when(betRepository.findByEventIdAndSettledFalse("EVT-1")).thenReturn(List.of(bet));

        betSettlementService.settle(new EventOutcomeMessage("EVT-1", "Fixture", null));
        simulateTransactionCommit();

        ArgumentCaptor<BetSettlementMessage> captor = ArgumentCaptor.forClass(BetSettlementMessage.class);
        verify(rocketMqProducer).sendSettlement(captor.capture());
        assertThat(captor.getValue().result()).isEqualTo(SettlementResult.LOST);
    }
}
