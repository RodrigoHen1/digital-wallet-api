package com.rodrigohen.wallet.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One line of a wallet statement, seen from that wallet's side: money coming in or going out.
 */
public record StatementEntry(UUID transactionId, TransactionType type, Direction direction, BigDecimal amount,
		UUID counterpartyWalletId, Instant createdAt) {

	public enum Direction {

		IN, OUT

	}

	static StatementEntry of(Transaction transaction, UUID walletId) {
		boolean outgoing = walletId.equals(transaction.getSourceWalletId());
		return new StatementEntry(transaction.getId(), transaction.getType(), outgoing ? Direction.OUT : Direction.IN,
				transaction.getAmount(), outgoing ? transaction.getTargetWalletId() : transaction.getSourceWalletId(),
				transaction.getCreatedAt());
	}

}
