package com.rodrigohen.wallet.wallet;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

	Optional<Wallet> findByOwnerId(UUID ownerId);

	/**
	 * Loads the wallet with {@code SELECT ... FOR UPDATE}: other transactions that want to change this wallet wait
	 * until the current one commits, so two transfers can never both spend the same money.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from Wallet w where w.id = :id")
	Optional<Wallet> findByIdForUpdate(UUID id);

}
