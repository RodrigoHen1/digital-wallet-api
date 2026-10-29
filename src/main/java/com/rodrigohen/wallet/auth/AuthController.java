package com.rodrigohen.wallet.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rodrigohen.wallet.common.ProblemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/token")
	@SecurityRequirements
	@Operation(summary = "Log in",
			description = "Exchanges email and password for a bearer token. Send it as `Authorization: Bearer <token>`.")
	@ApiResponse(responseCode = "200", description = "Token issued")
	@ProblemResponse.BadRequest
	@ApiResponse(responseCode = "401", description = "Wrong email or password")
	public TokenResponse token(@Valid @RequestBody TokenRequest request) {
		return authService.issueToken(request.email(), request.password());
	}

}
