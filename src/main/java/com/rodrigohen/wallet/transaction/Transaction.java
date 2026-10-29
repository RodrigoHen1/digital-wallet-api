package com.rodrigohen.wallet.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.rodrigohen.wallet.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An immutable record of money moving into or between wallets. Rows are only ever inserted, never updated.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TransactionType type;

	@Column(name = "source_wallet_id")
	private UUID sourceWalletId;

	@Column(name = "target_wallet_id", nullable = false)
	private UUID targetWalletId;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Transaction() {
		// required by JPA
	}

	private Transaction(TransactionType type, UUID sourceWalletId, UUID targetWalletId, BigDecimal amount,
			String idempotencyKey) {
		this.id = UUID.randomUUID();
		this.type = type;
		this.sourceWalletId = sourceWalletId;
		this.targetWalletId = targetWalletId;
		this.amount = amount;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = Timestamps.now();
	}

	static Transaction deposit(UUID walletId, BigDecimal amount, String idempotencyKey) {
		return new Transaction(TransactionType.DEPOSIT, null, walletId, amount, idempotencyKey);
	}

	static Transaction transfer(UUID sourceWalletId, UUID targetWalletId, BigDecimal amount, String idempotencyKey) {
		return new Transaction(TransactionType.TRANSFER, sourceWalletId, targetWalletId, amount, idempotencyKey);
	}

	/**
	 * Whether a retried request carries the same operation as the one originally stored under its idempotency key.
	 */
	boolean isSameOperation(TransactionType type, UUID sourceWalletId, UUID targetWalletId, BigDecimal amount) {
		return this.type == type && Objects.equals(this.sourceWalletId, sourceWalletId)
				&& this.targetWalletId.equals(targetWalletId) && this.amount.compareTo(amount) == 0;
	}

	public UUID getId() {
		return id;
	}

	public TransactionType getType() {
		return type;
	}

	public UUID getSourceWalletId() {
		return sourceWalletId;
	}

	public UUID getTargetWalletId() {
		return targetWalletId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
