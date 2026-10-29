package com.rodrigohen.wallet.wallet;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rodrigohen.wallet.common.NotFoundException;

@Service
public class WalletService {

	private final WalletRepository wallets;

	public WalletService(WalletRepository wallets) {
		this.wallets = wallets;
	}

	/**
	 * Someone else's wallet gets the same 404 as a wallet that does not exist, so wallet ids can't be probed.
	 */
	@Transactional(readOnly = true)
	public WalletResponse get(UUID id, UUID userId) {
		return wallets.findByIdAndOwnerId(id, userId)
			.map(WalletResponse::from)
			.orElseThrow(() -> new NotFoundException("Wallet " + id + " not found"));
	}

}
