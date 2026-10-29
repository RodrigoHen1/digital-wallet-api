package com.rodrigohen.wallet.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
class TransactionApiIntegrationTests {

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
	void depositIncreasesBalance() throws Exception {
		deposit(maria, maria.walletId(), "150.75", "dep-1").andExpect(status().isCreated())
			.andExpect(jsonPath("$.type").value("DEPOSIT"))
			.andExpect(jsonPath("$.amount").value(150.75));

		expectBalance(maria, 150.75);
	}

	@Test
	void transferMovesMoneyBetweenWallets() throws Exception {
		deposit(maria, maria.walletId(), "100.00", "dep-1").andExpect(status().isCreated());

		transfer(maria, maria.walletId(), joao.walletId(), "30.50", "tr-1").andExpect(status().isCreated())
			.andExpect(jsonPath("$.type").value("TRANSFER"));

		expectBalance(maria, 69.50);
		expectBalance(joao, 30.50);
	}

	@Test
	void rejectsTransferWithInsufficientBalance() throws Exception {
		deposit(maria, maria.walletId(), "10.00", "dep-1").andExpect(status().isCreated());

		transfer(maria, maria.walletId(), joao.walletId(), "10.01", "tr-1")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Insufficient balance"));

		expectBalance(maria, 10.00);
		expectBalance(joao, 0.00);
	}

	@Test
	void rejectsTransferToSameWallet() throws Exception {
		deposit(maria, maria.walletId(), "10.00", "dep-1").andExpect(status().isCreated());

		transfer(maria, maria.walletId(), maria.walletId(), "1.00", "tr-1")
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void retryWithSameIdempotencyKeyDoesNotMoveMoneyTwice() throws Exception {
		deposit(maria, maria.walletId(), "100.00", "dep-1").andExpect(status().isCreated());

		String first = transfer(maria, maria.walletId(), joao.walletId(), "40.00", "tr-1")
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String retry = transfer(maria, maria.walletId(), joao.walletId(), "40.00", "tr-1")
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(retry).isEqualTo(first);
		expectBalance(maria, 60.00);
		expectBalance(joao, 40.00);
	}

	@Test
	void rejectsIdempotencyKeyReusedForDifferentRequest() throws Exception {
		deposit(maria, maria.walletId(), "100.00", "dep-1").andExpect(status().isCreated());
		transfer(maria, maria.walletId(), joao.walletId(), "40.00", "tr-1").andExpect(status().isCreated());

		transfer(maria, maria.walletId(), joao.walletId(), "41.00", "tr-1")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Idempotency-Key was already used for a different request"));

		expectBalance(maria, 60.00);
	}

	@Test
	void idempotencyKeysAreScopedPerUser() throws Exception {
		// Both users happen to pick the same key: each deposit must still go through
		deposit(maria, maria.walletId(), "10.00", "same-key").andExpect(status().isCreated());
		deposit(joao, joao.walletId(), "20.00", "same-key").andExpect(status().isCreated());

		expectBalance(maria, 10.00);
		expectBalance(joao, 20.00);
	}

	@Test
	void requiresIdempotencyKeyHeader() throws Exception {
		mvc.perform(post("/wallets/{id}/deposits", maria.walletId()).header(HttpHeaders.AUTHORIZATION, maria.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": 10.00}")).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsInvalidAmounts() throws Exception {
		deposit(maria, maria.walletId(), "0", "dep-1").andExpect(status().isBadRequest());
		deposit(maria, maria.walletId(), "-5.00", "dep-2").andExpect(status().isBadRequest());
		deposit(maria, maria.walletId(), "1.001", "dep-3").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.amount").exists());
	}

	@Test
	void returnsNotFoundForUnknownWallet() throws Exception {
		deposit(maria, UUID.randomUUID(), "10.00", "dep-1").andExpect(status().isNotFound());
		transfer(maria, maria.walletId(), UUID.randomUUID(), "1.00", "tr-1").andExpect(status().isNotFound());
		mvc.perform(get("/wallets/{id}/transactions", UUID.randomUUID())
			.header(HttpHeaders.AUTHORIZATION, maria.bearer())).andExpect(status().isNotFound());
	}

	@Test
	void statementShowsEachWalletsSideNewestFirst() throws Exception {
		deposit(maria, maria.walletId(), "100.00", "dep-1").andExpect(status().isCreated());
		transfer(maria, maria.walletId(), joao.walletId(), "25.00", "tr-1").andExpect(status().isCreated());

		statement(maria).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalEntries").value(2))
			.andExpect(jsonPath("$.entries", hasSize(2)))
			.andExpect(jsonPath("$.entries[0].type").value("TRANSFER"))
			.andExpect(jsonPath("$.entries[0].direction").value("OUT"))
			.andExpect(jsonPath("$.entries[0].counterpartyWalletId").value(joao.walletId().toString()))
			.andExpect(jsonPath("$.entries[1].type").value("DEPOSIT"))
			.andExpect(jsonPath("$.entries[1].direction").value("IN"));

		statement(joao).andExpect(jsonPath("$.entries", hasSize(1)))
			.andExpect(jsonPath("$.entries[0].direction").value("IN"))
			.andExpect(jsonPath("$.entries[0].counterpartyWalletId").value(maria.walletId().toString()));
	}

	@Test
	void statementIsPaginated() throws Exception {
		for (int i = 1; i <= 5; i++) {
			deposit(maria, maria.walletId(), "1.00", "dep-" + i).andExpect(status().isCreated());
		}

		mvc.perform(get("/wallets/{id}/transactions", maria.walletId())
			.header(HttpHeaders.AUTHORIZATION, maria.bearer())
			.param("page", "1")
			.param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.entries", hasSize(2)))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalEntries").value(5))
			.andExpect(jsonPath("$.totalPages").value(3));

		mvc.perform(get("/wallets/{id}/transactions", maria.walletId())
			.header(HttpHeaders.AUTHORIZATION, maria.bearer())
			.param("size", "500")).andExpect(status().isBadRequest());
	}

	private ResultActions deposit(SignedUpUser caller, UUID walletId, String amount, String key) throws Exception {
		return mvc.perform(post("/wallets/{id}/deposits", walletId).header(HttpHeaders.AUTHORIZATION, caller.bearer())
			.header("Idempotency-Key", key)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\": " + amount + "}"));
	}

	private ResultActions transfer(SignedUpUser caller, UUID source, UUID target, String amount, String key)
			throws Exception {
		String json = """
				{"sourceWalletId": "%s", "targetWalletId": "%s", "amount": %s}
				""".formatted(source, target, amount);
		return mvc.perform(post("/transfers").header(HttpHeaders.AUTHORIZATION, caller.bearer())
			.header("Idempotency-Key", key)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private ResultActions statement(SignedUpUser caller) throws Exception {
		return mvc.perform(get("/wallets/{id}/transactions", caller.walletId())
			.header(HttpHeaders.AUTHORIZATION, caller.bearer()));
	}

	private void expectBalance(SignedUpUser owner, double expected) throws Exception {
		mvc.perform(get("/wallets/{id}", owner.walletId()).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
			.andExpect(jsonPath("$.balance").value(expected));
	}

}
