package com.rodrigohen.wallet.transaction;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rodrigohen.wallet.common.BusinessRuleException;
import com.rodrigohen.wallet.common.NotFoundException;
import com.rodrigohen.wallet.wallet.Wallet;
import com.rodrigohen.wallet.wallet.WalletRepository;

@Service
public class TransactionService {

	private final TransactionRepository transactions;

	private final WalletRepository wallets;

	public TransactionService(TransactionRepository transactions, WalletRepository wallets) {
		this.transactions = transactions;
		this.wallets = wallets;
	}

	/**
	 * Adds money to one of the user's own wallets.
	 */
	@Transactional
	public TransactionResult deposit(UUID userId, UUID walletId, BigDecimal amount, String idempotencyKey) {
		Optional<TransactionResult> replay = replay(userId, idempotencyKey, TransactionType.DEPOSIT, null, walletId,
				amount);
		if (replay.isPresent()) {
			return replay.get();
		}

		Wallet wallet = lockWallet(walletId);
		requireOwner(wallet, userId);
		wallet.credit(amount);
		return created(transactions.save(Transaction.deposit(walletId, amount, userId, idempotencyKey)));
	}

	/**
	 * Moves money from one of the user's own wallets to any other wallet.
	 */
	@Transactional
	public TransactionResult transfer(UUID userId, UUID sourceWalletId, UUID targetWalletId, BigDecimal amount,
			String idempotencyKey) {
		Optional<TransactionResult> replay = replay(userId, idempotencyKey, TransactionType.TRANSFER, sourceWalletId,
				targetWalletId, amount);
		if (replay.isPresent()) {
			return replay.get();
		}
		if (sourceWalletId.equals(targetWalletId)) {
			throw new BusinessRuleException("Cannot transfer to the same wallet");
		}

		// Always lock the two wallets in the same order (lowest id first). If A->B locked A then B while B->A
		// locked B then A, each would wait for the other forever (a deadlock).
		boolean sourceFirst = sourceWalletId.compareTo(targetWalletId) < 0;
		Wallet first = lockWallet(sourceFirst ? sourceWalletId : targetWalletId);
		Wallet second = lockWallet(sourceFirst ? targetWalletId : sourceWalletId);
		Wallet source = sourceFirst ? first : second;
		Wallet target = sourceFirst ? second : first;

		requireOwner(source, userId);
		source.debit(amount);
		target.credit(amount);
		return created(transactions
			.save(Transaction.transfer(sourceWalletId, targetWalletId, amount, userId, idempotencyKey)));
	}

	@Transactional(readOnly = true)
	public StatementPage statement(UUID userId, UUID walletId, int page, int size) {
		if (!wallets.existsByIdAndOwnerId(walletId, userId)) {
			throw walletNotFound(walletId);
		}
		PageRequest pageRequest = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return StatementPage
			.from(transactions.findByWallet(walletId, pageRequest).map(t -> StatementEntry.of(t, walletId)));
	}

	/**
	 * If this user already used the idempotency key, returns the original result instead of moving money again.
	 * Reusing a key for a different operation is a client bug, so it is rejected rather than silently ignored.
	 */
	private Optional<TransactionResult> replay(UUID userId, String idempotencyKey, TransactionType type,
			UUID sourceWalletId, UUID targetWalletId, BigDecimal amount) {
		return transactions.findByInitiatedByAndIdempotencyKey(userId, idempotencyKey).map(existing -> {
			if (!existing.isSameOperation(type, sourceWalletId, targetWalletId, amount)) {
				throw new BusinessRuleException("Idempotency-Key was already used for a different request");
			}
			return new TransactionResult(TransactionResponse.from(existing), false);
		});
	}

	private Wallet lockWallet(UUID walletId) {
		return wallets.findByIdForUpdate(walletId).orElseThrow(() -> walletNotFound(walletId));
	}

	// Someone else's wallet is reported exactly like a missing one, so wallet ids can't be probed
	private static void requireOwner(Wallet wallet, UUID userId) {
		if (!wallet.getOwner().getId().equals(userId)) {
			throw walletNotFound(wallet.getId());
		}
	}

	private static NotFoundException walletNotFound(UUID walletId) {
		return new NotFoundException("Wallet " + walletId + " not found");
	}

	private static TransactionResult created(Transaction transaction) {
		return new TransactionResult(TransactionResponse.from(transaction), true);
	}

}
