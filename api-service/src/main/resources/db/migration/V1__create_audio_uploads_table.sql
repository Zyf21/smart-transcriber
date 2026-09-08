CREATE TABLE audio_uploads (
    id                UUID PRIMARY KEY,
    original_filename VARCHAR(255)  NOT NULL,
    content_type      VARCHAR(128),
    file_size_bytes   BIGINT,
    bucket            VARCHAR(255)  NOT NULL,
    object_key        VARCHAR(512)  NOT NULL UNIQUE,
    status            VARCHAR(32)   NOT NULL,
    error_message     VARCHAR(500),
    questions         TEXT,
    completed_at      TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audio_uploads_status_created_at ON audio_uploads (status, created_at);
