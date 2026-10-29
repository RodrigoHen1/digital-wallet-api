package com.rodrigohen.wallet.auth;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.rodrigohen.wallet.TestUsers;
import com.rodrigohen.wallet.TestUsers.SignedUpUser;
import com.rodrigohen.wallet.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	private SignedUpUser maria;

	private SignedUpUser joao;

	@BeforeEach
	void setUp() throws Exception {
		jdbc.execute("TRUNCATE transactions, wallets, users");
		maria = TestUsers.signUp(mvc, "Maria Silva", "52998224725", "maria@example.com");
		joao = TestUsers.signUp(mvc, "Joao Souza", "11144477735", "joao@example.com");
	}

	@Test
	void issuesBearerTokenForValidCredentials() throws Exception {
		requestToken("MARIA@example.com", TestUsers.PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
		requestToken("maria@example.com", "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid email or password"));
		requestToken("nobody@example.com", TestUsers.PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void rejectsRequestsWithoutToken() throws Exception {
		mvc.perform(get("/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
			.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void rejectsTamperedToken() throws Exception {
		String tampered = maria.token().substring(0, maria.token().length() - 2) + "xx";

		mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void healthCheckIsPublic() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void cannotSeeAnotherUsersWallet() throws Exception {
		mvc.perform(get("/wallets/{id}", joao.walletId()).header(HttpHeaders.AUTHORIZATION, maria.bearer()))
			.andExpect(status().isNotFound());
		mvc.perform(get("/wallets/{id}/transactions", joao.walletId())
			.header(HttpHeaders.AUTHORIZATION, maria.bearer())).andExpect(status().isNotFound());
	}

	@Test
	void cannotDepositIntoAnotherUsersWallet() throws Exception {
		mvc.perform(post("/wallets/{id}/deposits", joao.walletId()).header(HttpHeaders.AUTHORIZATION, maria.bearer())
			.header("Idempotency-Key", "dep-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": 10.00}")).andExpect(status().isNotFound());
	}

	@Test
	void cannotTransferOutOfAnotherUsersWallet() throws Exception {
		mvc.perform(post("/wallets/{id}/deposits", joao.walletId()).header(HttpHeaders.AUTHORIZATION, joao.bearer())
			.header("Idempotency-Key", "dep-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": 50.00}")).andExpect(status().isCreated());

		// Maria tries to move Joao's money into her own wallet
		mvc.perform(post("/transfers").header(HttpHeaders.AUTHORIZATION, maria.bearer())
			.header("Idempotency-Key", "steal-1")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"sourceWalletId": "%s", "targetWalletId": "%s", "amount": 50.00}
					""".formatted(joao.walletId(), maria.walletId())))
			.andExpect(status().isNotFound());

		mvc.perform(get("/wallets/{id}", joao.walletId()).header(HttpHeaders.AUTHORIZATION, joao.bearer()))
			.andExpect(jsonPath("$.balance").value(50.00));
	}

	private ResultActions requestToken(String email, String password) throws Exception {
		return mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}
				""".formatted(email, password)));
	}

}
