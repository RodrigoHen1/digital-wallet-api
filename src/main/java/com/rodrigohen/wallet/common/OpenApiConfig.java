package com.rodrigohen.wallet.common;

import java.util.List;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rodrigohen.wallet.auth.CurrentUserId;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Describes the API for the interactive docs at /swagger-ui.html (spec at /v3/api-docs).
 */
@Configuration
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	static {
		// The logged-in user comes from the token, not from the request, so it must not appear as a parameter
		SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUserId.class);
	}

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info().title("Digital Wallet API")
				.version("v1")
				.description("""
						Wallets, deposits and transfers between users.

						**Try it:** sign up with `POST /users`, get a token from `POST /auth/token`, \
						click **Authorize** and paste the `accessToken`. Deposits and transfers need an \
						`Idempotency-Key` header: any unique value, e.g. a UUID."""))
			// Listed in the order a client uses them
			.tags(List.of(new Tag().name("Users"), new Tag().name("Authentication"), new Tag().name("Wallets"),
					new Tag().name("Transactions")))
			.components(new Components().addSecuritySchemes(BEARER_AUTH,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			// Every endpoint needs the token unless it opts out with an empty @SecurityRequirements
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

}
