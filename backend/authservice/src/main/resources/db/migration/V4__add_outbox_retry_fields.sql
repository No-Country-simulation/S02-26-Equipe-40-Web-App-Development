ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0;

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ;

UPDATE outbox_events
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_events_next_attempt_at ON outbox_events (next_attempt_at);
