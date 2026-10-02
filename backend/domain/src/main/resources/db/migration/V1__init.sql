CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE personal_access_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    token_prefix VARCHAR(16) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    scopes VARCHAR(255) NOT NULL DEFAULT 'capture:write,read',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pat_user ON personal_access_token(user_id);

CREATE TABLE category (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    icon VARCHAR(32),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, name)
);

CREATE TABLE capture (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    source VARCHAR(64) NOT NULL,
    raw_text TEXT,
    source_url TEXT,
    conversation_title VARCHAR(255),
    image_object_key VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    client_capture_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_capture_client_id ON capture(user_id, client_capture_id) WHERE client_capture_id IS NOT NULL;
CREATE INDEX idx_capture_user_status ON capture(user_id, status);

CREATE TABLE item (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    capture_id UUID NOT NULL REFERENCES capture(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    confidence DOUBLE PRECISION,
    embedding vector(384),
    embedding_model VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_item_user ON item(user_id);
CREATE INDEX idx_item_capture ON item(capture_id);
CREATE INDEX idx_item_embedding ON item USING hnsw (embedding vector_cosine_ops);

CREATE TABLE item_category (
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES category(id) ON DELETE CASCADE,
    PRIMARY KEY (item_id, category_id)
);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_event(published_at) WHERE published_at IS NULL;

CREATE TABLE user_quota (
    user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    daily_token_budget INT NOT NULL DEFAULT 50000,
    tokens_used_today INT NOT NULL DEFAULT 0,
    quota_day DATE NOT NULL DEFAULT CURRENT_DATE,
    max_capture_chars INT NOT NULL DEFAULT 50000
);

CREATE TABLE proposed_category (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL
);
