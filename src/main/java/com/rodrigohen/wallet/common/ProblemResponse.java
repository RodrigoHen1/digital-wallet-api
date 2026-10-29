package com.rodrigohen.wallet.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.http.ProblemDetail;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Shorthand for documenting an error response in the API's problem-detail format.
 */
public final class ProblemResponse {

	private ProblemResponse() {
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponse(responseCode = "400", description = "Invalid input",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	public @interface BadRequest {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponse(responseCode = "401", description = "Missing or invalid token",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	public @interface Unauthorized {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponse(responseCode = "404", description = "Wallet not found, or it belongs to someone else",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	public @interface WalletNotFound {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponse(responseCode = "422",
			description = "Business rule violated: insufficient balance, same wallet, or Idempotency-Key reused "
					+ "for a different request",
			content = @Content(mediaType = "application/problem+json",
					schema = @Schema(implementation = ProblemDetail.class)))
	public @interface BusinessRule {

	}

}
