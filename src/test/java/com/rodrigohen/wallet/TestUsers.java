package com.rodrigohen.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Signs users up and logs them in through the real API, for tests that need an authenticated caller.
 */
public final class TestUsers {

	public static final String PASSWORD = "correct-horse-battery";

	private TestUsers() {
	}

	public static SignedUpUser signUp(MockMvc mvc, String fullName, String cpf, String email) throws Exception {
		String body = mvc
			.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
					{"fullName": "%s", "document": "%s", "email": "%s", "password": "%s"}
					""".formatted(fullName, cpf, email, PASSWORD)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return new SignedUpUser(UUID.fromString(JsonPath.read(body, "$.id")),
				UUID.fromString(JsonPath.read(body, "$.walletId")), login(mvc, email, PASSWORD));
	}

	public static String login(MockMvc mvc, String email, String password) throws Exception {
		String body = mvc
			.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content("""
					{"email": "%s", "password": "%s"}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	public record SignedUpUser(UUID userId, UUID walletId, String token) {

		public String bearer() {
			return "Bearer " + token;
		}

	}

}
