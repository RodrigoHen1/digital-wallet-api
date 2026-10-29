CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    full_name  VARCHAR(120) NOT NULL,
    document   VARCHAR(11)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_document UNIQUE (document),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

CREATE TABLE wallets (
    id         UUID           PRIMARY KEY,
    user_id    UUID           NOT NULL REFERENCES users (id),
    balance    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    version    BIGINT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_wallets_user            UNIQUE (user_id),
    CONSTRAINT ck_wallets_balance_ge_zero CHECK (balance >= 0)
);
