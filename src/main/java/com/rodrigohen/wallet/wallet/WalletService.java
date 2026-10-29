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

	@Transactional(readOnly = true)
	public WalletResponse get(UUID id) {
		return wallets.findById(id)
			.map(WalletResponse::from)
			.orElseThrow(() -> new NotFoundException("Wallet " + id + " not found"));
	}

}
