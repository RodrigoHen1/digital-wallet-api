package com.rodrigohen.wallet.user;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rodrigohen.wallet.common.ConflictException;
import com.rodrigohen.wallet.common.NotFoundException;
import com.rodrigohen.wallet.wallet.Wallet;
import com.rodrigohen.wallet.wallet.WalletRepository;

@Service
public class UserService {

	private final UserRepository users;

	private final WalletRepository wallets;

	public UserService(UserRepository users, WalletRepository wallets) {
		this.users = users;
		this.wallets = wallets;
	}

	/**
	 * Creates the user and their wallet in one transaction: either both exist or neither does.
	 */
	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String document = request.document().replaceAll("\\D", "");
		String email = request.email().trim().toLowerCase(Locale.ROOT);

		if (users.existsByDocument(document)) {
			throw new ConflictException("A user with this CPF already exists");
		}
		if (users.existsByEmail(email)) {
			throw new ConflictException("A user with this email already exists");
		}

		User user = users.save(new User(request.fullName().trim(), document, email));
		Wallet wallet = wallets.save(new Wallet(user));
		return UserResponse.from(user, wallet);
	}

	@Transactional(readOnly = true)
	public UserResponse get(UUID id) {
		User user = users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
		Wallet wallet = wallets.findByOwnerId(id)
			.orElseThrow(() -> new IllegalStateException("User " + id + " has no wallet"));
		return UserResponse.from(user, wallet);
	}

}
