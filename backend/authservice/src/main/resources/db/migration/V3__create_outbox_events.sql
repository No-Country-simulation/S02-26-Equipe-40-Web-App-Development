CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL UNIQUE,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    error_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created_at ON outbox_events (status, created_at);
