package com.rodrigohen.wallet.user;

import java.time.Instant;
import java.util.UUID;

import com.rodrigohen.wallet.wallet.Wallet;

public record UserResponse(UUID id, String fullName, String email, UUID walletId, Instant createdAt) {

	static UserResponse from(User user, Wallet wallet) {
		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), wallet.getId(),
				user.getCreatedAt());
	}

}
