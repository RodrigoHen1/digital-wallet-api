package com.rodrigohen.wallet.auth;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rodrigohen.wallet.common.UnauthorizedException;
import com.rodrigohen.wallet.user.User;
import com.rodrigohen.wallet.user.UserRepository;

@Service
public class AuthService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final JwtEncoder jwtEncoder;

	private final JwtProperties properties;

	// Compared against when the email is unknown, so a wrong email takes as long as a wrong password and response
	// times do not reveal which emails are registered
	private final String dummyHash;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			JwtProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.dummyHash = passwordEncoder.encode("not-a-real-password");
	}

	@Transactional(readOnly = true)
	public TokenResponse issueToken(String email, String password) {
		User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
		String hash = (user != null) ? user.getPasswordHash() : dummyHash;
		boolean matches = passwordEncoder.matches(password, hash);
		if (user == null || !matches) {
			// Same message for both cases, again so the response does not reveal whether the email exists
			throw new UnauthorizedException("Invalid email or password");
		}

		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("digital-wallet-api")
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(properties.ttl()))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", properties.ttl().toSeconds());
	}

}
