package com.rodrigohen.wallet.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rodrigohen.wallet.common.ConflictException;
import com.rodrigohen.wallet.wallet.Wallet;
import com.rodrigohen.wallet.wallet.WalletRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

	@Mock
	private UserRepository users;

	@Mock
	private WalletRepository wallets;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	@Test
	void storesNormalizedCpfAndEmailAndHashedPassword() {
		when(passwordEncoder.encode("secret-password")).thenReturn("{bcrypt}hashed");
		when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(wallets.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userService.create(new CreateUserRequest("  Maria Silva ", "529.982.247-25", " Maria@Example.COM ",
				"secret-password"));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).save(saved.capture());
		assertThat(saved.getValue().getDocument()).isEqualTo("52998224725");
		assertThat(saved.getValue().getEmail()).isEqualTo("maria@example.com");
		assertThat(saved.getValue().getFullName()).isEqualTo("Maria Silva");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("{bcrypt}hashed");
	}

	@Test
	void doesNotCreateAnythingWhenCpfIsTaken() {
		when(users.existsByDocument("52998224725")).thenReturn(true);

		assertThatThrownBy(() -> userService
			.create(new CreateUserRequest("Maria", "529.982.247-25", "maria@example.com", "secret-password")))
			.isInstanceOf(ConflictException.class);

		verify(users, never()).save(any());
		verify(wallets, never()).save(any());
	}

}
