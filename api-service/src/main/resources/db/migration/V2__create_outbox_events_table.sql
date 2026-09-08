CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    aggregate_id  UUID         NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    error_message VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    sent_at       TIMESTAMP
);

CREATE INDEX idx_outbox_events_status_created_at ON outbox_events (status, created_at);
