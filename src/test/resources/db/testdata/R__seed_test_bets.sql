INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-1001', 'USR-1', 'EVT-1001', 'MKT-1', 'TEAM-A', 25.50);

INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-1002', 'USR-2', 'EVT-1001', 'MKT-1', 'TEAM-B', 10.00);

INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-1003', 'USR-3', 'EVT-2002', 'MKT-7', 'TEAM-X', 14.75);

-- EventOutcomeFlowIntegrationTest.shouldPublishConsumeAndEmitSettlementMessages
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-3001', 'USR-1', 'EVT-3003', 'MKT-1', 'TEAM-A', 25.50);
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-3002', 'USR-2', 'EVT-3003', 'MKT-1', 'TEAM-B', 10.00);

-- EventOutcomeFlowIntegrationTest.shouldEmitWonAndLostSettlementPayloads
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-4001', 'USR-1', 'EVT-4004', 'MKT-1', 'TEAM-A', 25.50);
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-4002', 'USR-2', 'EVT-4004', 'MKT-1', 'TEAM-B', 10.00);

-- EventOutcomeFlowIntegrationTest.shouldNotSettleSameBetsTwiceOnDuplicateEvent
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-5001', 'USR-1', 'EVT-5005', 'MKT-1', 'TEAM-A', 25.50);
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-5002', 'USR-2', 'EVT-5005', 'MKT-1', 'TEAM-B', 10.00);

-- ObservabilityIntegrationTest.shouldExposeCustomMicrometerCountersAfterProcessingAnEvent
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-6001', 'USR-1', 'EVT-6006', 'MKT-1', 'TEAM-A', 25.50);

-- ConcurrentSettlementIntegrationTest
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-7001', 'USR-1', 'EVT-7007', 'MKT-1', 'TEAM-A', 25.50);
INSERT INTO bets (bet_id, user_id, event_id, event_market_id, selected_winner_id, bet_amount)
VALUES ('BET-7002', 'USR-2', 'EVT-7007', 'MKT-1', 'TEAM-B', 10.00);
