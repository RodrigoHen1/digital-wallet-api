package com.rodrigohen.wallet.wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.rodrigohen.wallet.common.BusinessRuleException;
import com.rodrigohen.wallet.common.Timestamps;
import com.rodrigohen.wallet.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "wallets")
public class Wallet {

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User owner;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance;

	// Optimistic locking: a concurrent update to the same wallet fails instead of silently overwriting it
	@Version
	private long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Wallet() {
		// required by JPA
	}

	public Wallet(User owner) {
		this.id = UUID.randomUUID();
		this.owner = owner;
		this.balance = BigDecimal.ZERO.setScale(2);
		this.createdAt = Timestamps.now();
	}

	public void credit(BigDecimal amount) {
		this.balance = this.balance.add(amount);
	}

	public void debit(BigDecimal amount) {
		if (this.balance.compareTo(amount) < 0) {
			throw new BusinessRuleException("Insufficient balance");
		}
		this.balance = this.balance.subtract(amount);
	}

	public UUID getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
