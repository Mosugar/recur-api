-- V1: initial schema

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Users ──────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    main_currency VARCHAR(3)  NOT NULL DEFAULT 'EUR',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_currency CHECK (main_currency IN ('EUR','USD','GBP','MAD'))
);

-- ── Subscriptions ──────────────────────────────────────────────────────────────
CREATE TABLE subscriptions (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID        NOT NULL,
    name                VARCHAR(150) NOT NULL,
    price_cents         BIGINT       NOT NULL,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    cycle               VARCHAR(20)  NOT NULL,
    first_payment_date  DATE         NOT NULL,
    category            VARCHAR(30)  NOT NULL DEFAULT 'OTHER',
    color               VARCHAR(7)   NOT NULL DEFAULT '#6366F1',
    notes               TEXT,
    reminder_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_subscriptions        PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_subscriptions_currency CHECK (currency IN ('EUR','USD','GBP','MAD')),
    CONSTRAINT ck_subscriptions_cycle    CHECK (cycle IN ('WEEKLY','MONTHLY','QUARTERLY','YEARLY')),
    CONSTRAINT ck_subscriptions_category CHECK (category IN ('STREAMING','MUSIC','SOFTWARE','GAMING','FITNESS','CLOUD','NEWS','OTHER')),
    CONSTRAINT ck_subscriptions_price   CHECK (price_cents >= 0)
);

-- Indexes for common query patterns
CREATE INDEX idx_subscriptions_user_id        ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_user_active    ON subscriptions (user_id, active);
CREATE INDEX idx_subscriptions_user_category  ON subscriptions (user_id, category);
