CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE webhooks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    url                 VARCHAR(500) NOT NULL,
    secret              VARCHAR(200) NOT NULL,
    max_attempts        INT NOT NULL DEFAULT 5,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhooks_tenant ON webhooks(tenant_id);

CREATE TABLE deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    webhook_id          UUID NOT NULL REFERENCES webhooks(id),
    idempotency_key     VARCHAR(200) NOT NULL,
    payload             JSONB NOT NULL,
    status              VARCHAR(30) NOT NULL,
    attempts_count      INT NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_delivery_idempotency UNIQUE (webhook_id, idempotency_key)
);

CREATE INDEX idx_deliveries_status_next ON deliveries(status, next_attempt_at);
CREATE INDEX idx_deliveries_tenant ON deliveries(tenant_id);

CREATE TABLE attempts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id     UUID NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    attempt_number  INT NOT NULL,
    status_code     INT,
    error_message   TEXT,
    latency_ms      INT,
    attempted_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attempts_delivery ON attempts(delivery_id);

CREATE TABLE dead_letters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id     UUID NOT NULL REFERENCES deliveries(id),
    reason          TEXT NOT NULL,
    moved_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    replayed_at     TIMESTAMPTZ
);

CREATE INDEX idx_dead_letters_delivery ON dead_letters(delivery_id);