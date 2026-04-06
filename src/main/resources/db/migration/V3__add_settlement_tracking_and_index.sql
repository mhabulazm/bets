ALTER TABLE bets ADD COLUMN settled BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_bets_event_id ON bets(event_id);
