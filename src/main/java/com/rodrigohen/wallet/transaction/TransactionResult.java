package com.rodrigohen.wallet.transaction;

/**
 * A transaction plus whether this request created it, or replayed one already stored under the same idempotency key.
 */
public record TransactionResult(TransactionResponse transaction, boolean created) {
}
