CREATE TABLE transactions (
    id               UUID           PRIMARY KEY,
    type             VARCHAR(16)    NOT NULL,
    source_wallet_id UUID           REFERENCES wallets (id),
    target_wallet_id UUID           NOT NULL REFERENCES wallets (id),
    amount           NUMERIC(19, 2) NOT NULL,
    idempotency_key  VARCHAR(64)    NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (type IN ('DEPOSIT', 'TRANSFER')),
    -- A deposit has no source wallet; a transfer must have a source that differs from the target
    CONSTRAINT ck_transactions_source CHECK (
        (type = 'DEPOSIT' AND source_wallet_id IS NULL)
        OR (type = 'TRANSFER' AND source_wallet_id IS NOT NULL AND source_wallet_id <> target_wallet_id)
    )
);

-- The statement endpoint looks up a wallet's transactions newest first, from either side
CREATE INDEX ix_transactions_source_wallet ON transactions (source_wallet_id, created_at DESC);
CREATE INDEX ix_transactions_target_wallet ON transactions (target_wallet_id, created_at DESC);
