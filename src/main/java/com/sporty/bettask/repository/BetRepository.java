package com.sporty.bettask.repository;

import com.sporty.bettask.entity.Bet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, String> {

    List<Bet> findByEventIdAndSettledFalse(String eventId);
}

