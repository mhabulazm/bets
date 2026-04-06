package com.sporty.bettask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "bets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bet {

    @Id
    @Column(nullable = false, updatable = false)
    private String betId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String eventMarketId;

    @Column(nullable = false)
    private String selectedWinnerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal betAmount;

    @Column(nullable = false)
    private boolean settled = false;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Bet(String betId, String userId, String eventId, String eventMarketId, String selectedWinnerId, BigDecimal betAmount) {
        this.betId = betId;
        this.userId = userId;
        this.eventId = eventId;
        this.eventMarketId = eventMarketId;
        this.selectedWinnerId = selectedWinnerId;
        this.betAmount = betAmount;
    }

    public void markSettled() {
        this.settled = true;
    }
}
