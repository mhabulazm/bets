package com.sporty.bettask.integration;

import com.sporty.bettask.domain.EventOutcomeMessage;
import com.sporty.bettask.messaging.RocketMqProducer;
import com.sporty.bettask.service.BetSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ConcurrentSettlementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BetSettlementService betSettlementService;

    @MockitoSpyBean
    private RocketMqProducer rocketMqProducer;

    @BeforeEach
    void setUp() {
        reset(rocketMqProducer);
    }

    @Test
    void shouldSettleBetsExactlyOnceUnderConcurrentServiceCalls() throws Exception {
        EventOutcomeMessage outcome = new EventOutcomeMessage("EVT-7007", "Team A vs Team B", "TEAM-A");
        int threads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger optimisticLockFailures = new AtomicInteger();
        AtomicInteger successes = new AtomicInteger();

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        betSettlementService.settle(outcome);
                        successes.incrementAndGet();
                    } catch (Exception e) {
                        if (e.getClass().getSimpleName().contains("OptimisticLocking")) {
                            optimisticLockFailures.incrementAndGet();
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
        }

        // Exactly one thread should succeed in settling, the rest either get
        // an optimistic lock conflict or find no unsettled bets.
        assertThat(successes.get() + optimisticLockFailures.get()).isEqualTo(threads);

        // EVT-7007 has 2 bets — exactly 2 settlement messages should be emitted.
        verify(rocketMqProducer, times(2)).sendSettlement(any());
    }
}
