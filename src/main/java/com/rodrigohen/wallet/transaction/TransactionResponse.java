package com.rodrigohen.wallet.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id, TransactionType type, UUID sourceWalletId, UUID targetWalletId,
		BigDecimal amount, Instant createdAt) {

	static TransactionResponse from(Transaction transaction) {
		return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getSourceWalletId(),
				transaction.getTargetWalletId(), transaction.getAmount(), transaction.getCreatedAt());
	}

}
