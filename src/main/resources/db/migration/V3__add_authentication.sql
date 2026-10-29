-- Users created before authentication existed get '!', which never matches a password hash, so they cannot log in
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100) NOT NULL DEFAULT '!';
ALTER TABLE users ALTER COLUMN password_hash DROP DEFAULT;

-- Who made each transaction. Idempotency keys become unique per user instead of globally,
-- so two different users can never collide on the same key
ALTER TABLE transactions ADD COLUMN initiated_by UUID REFERENCES users (id);

UPDATE transactions t
SET initiated_by = w.user_id
FROM wallets w
WHERE w.id = COALESCE(t.source_wallet_id, t.target_wallet_id);

ALTER TABLE transactions ALTER COLUMN initiated_by SET NOT NULL;
ALTER TABLE transactions DROP CONSTRAINT uk_transactions_idempotency_key;
ALTER TABLE transactions ADD CONSTRAINT uk_transactions_initiator_idempotency_key UNIQUE (initiated_by, idempotency_key);
