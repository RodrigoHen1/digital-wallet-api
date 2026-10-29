package com.rodrigohen.wallet.wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(UUID id, UUID ownerId, BigDecimal balance) {

	static WalletResponse from(Wallet wallet) {
		return new WalletResponse(wallet.getId(), wallet.getOwner().getId(), wallet.getBalance());
	}

}
