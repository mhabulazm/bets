package com.sporty.bettask.messaging;

import com.sporty.bettask.domain.BetSettlementMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingRocketMqProducer implements RocketMqProducer {

    private final String topic;

    public LoggingRocketMqProducer(@Value("${app.rocketmq.bet-settlements-topic}") String topic) {
        this.topic = topic;
    }

    @Override
    public void sendSettlement(BetSettlementMessage message) {
        log.debug("Mock RocketMQ send topic={} betId={} eventId={} result={}",
                topic, message.betId(), message.eventId(), message.result());
    }
}
