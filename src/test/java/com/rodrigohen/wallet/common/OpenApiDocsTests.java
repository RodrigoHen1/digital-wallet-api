package com.rodrigohen.wallet.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.rodrigohen.wallet.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocsTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void specIsPublicAndDescribesBearerAuth() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("Digital Wallet API"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.paths['/transfers'].post").exists())
			.andExpect(jsonPath("$.paths['/auth/token'].post").exists());
	}

	@Test
	void publicEndpointsDoNotAskForToken() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/users'].post.security", empty()))
			.andExpect(jsonPath("$.paths['/auth/token'].post.security", empty()));
	}

	@Test
	void documentsIdempotencyKeyButNotTheLoggedInUser() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/transfers'].post.parameters[*].name", hasItem("Idempotency-Key")))
			.andExpect(jsonPath("$.paths['/transfers'].post.parameters[*].name", not(hasItem("userId"))))
			.andExpect(jsonPath("$.paths['/transfers'].post.responses['422'].description",
					containsString("insufficient balance")));
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/swagger-ui/index.html"));
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

}
