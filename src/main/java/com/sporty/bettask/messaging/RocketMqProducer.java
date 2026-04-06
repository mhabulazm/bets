package com.sporty.bettask.messaging;

import com.sporty.bettask.domain.BetSettlementMessage;

public interface RocketMqProducer {

    void sendSettlement(BetSettlementMessage message);
}

