package com.rodrigohen.wallet.user;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rodrigohen.wallet.auth.CurrentUserId;
import com.rodrigohen.wallet.common.ProblemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@SecurityRequirements
	@Operation(summary = "Sign up", description = "Creates a user together with an empty wallet.")
	@ApiResponse(responseCode = "201", description = "User and wallet created")
	@ProblemResponse.BadRequest
	@ApiResponse(responseCode = "409", description = "CPF or email already registered")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse created = userService.create(request);
		return ResponseEntity.created(URI.create("/users/me")).body(created);
	}

	@GetMapping("/me")
	@Operation(summary = "Get the logged-in user")
	@ProblemResponse.Unauthorized
	public UserResponse me(@CurrentUserId UUID userId) {
		return userService.get(userId);
	}

}
