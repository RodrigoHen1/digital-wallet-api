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

	@Transactional
	public TransactionResult deposit(UUID walletId, BigDecimal amount, String idempotencyKey) {
		Optional<TransactionResult> replay = replay(idempotencyKey, TransactionType.DEPOSIT, null, walletId, amount);
		if (replay.isPresent()) {
			return replay.get();
		}

		Wallet wallet = lockWallet(walletId);
		wallet.credit(amount);
		return created(transactions.save(Transaction.deposit(walletId, amount, idempotencyKey)));
	}

	@Transactional
	public TransactionResult transfer(UUID sourceWalletId, UUID targetWalletId, BigDecimal amount,
			String idempotencyKey) {
		Optional<TransactionResult> replay = replay(idempotencyKey, TransactionType.TRANSFER, sourceWalletId,
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

		source.debit(amount);
		target.credit(amount);
		return created(transactions.save(Transaction.transfer(sourceWalletId, targetWalletId, amount, idempotencyKey)));
	}

	@Transactional(readOnly = true)
	public StatementPage statement(UUID walletId, int page, int size) {
		if (!wallets.existsById(walletId)) {
			throw new NotFoundException("Wallet " + walletId + " not found");
		}
		PageRequest pageRequest = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		return StatementPage
			.from(transactions.findByWallet(walletId, pageRequest).map(t -> StatementEntry.of(t, walletId)));
	}

	/**
	 * If this idempotency key was used before, returns the original result instead of moving money again. Reusing a
	 * key for a different operation is a client bug, so it is rejected rather than silently ignored.
	 */
	private Optional<TransactionResult> replay(String idempotencyKey, TransactionType type, UUID sourceWalletId,
			UUID targetWalletId, BigDecimal amount) {
		return transactions.findByIdempotencyKey(idempotencyKey).map(existing -> {
			if (!existing.isSameOperation(type, sourceWalletId, targetWalletId, amount)) {
				throw new BusinessRuleException("Idempotency-Key was already used for a different request");
			}
			return new TransactionResult(TransactionResponse.from(existing), false);
		});
	}

	private Wallet lockWallet(UUID walletId) {
		return wallets.findByIdForUpdate(walletId)
			.orElseThrow(() -> new NotFoundException("Wallet " + walletId + " not found"));
	}

	private static TransactionResult created(Transaction transaction) {
		return new TransactionResult(TransactionResponse.from(transaction), true);
	}

}
