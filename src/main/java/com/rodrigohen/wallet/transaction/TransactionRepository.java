package com.rodrigohen.wallet.transaction;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

	@Query("select t from Transaction t where t.sourceWalletId = :walletId or t.targetWalletId = :walletId")
	Page<Transaction> findByWallet(UUID walletId, Pageable pageable);

}
