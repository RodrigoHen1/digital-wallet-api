package com.rodrigohen.wallet.user;

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
class UserApiIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void cleanDatabase() {
		jdbc.execute("TRUNCATE transactions, wallets, users");
	}

	@Test
	void createsUserWithEmptyWallet() throws Exception {
		createUser("Maria Silva", "529.982.247-25", "Maria@Example.com", TestUsers.PASSWORD)
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/users/me"))
			.andExpect(jsonPath("$.fullName").value("Maria Silva"))
			.andExpect(jsonPath("$.email").value("maria@example.com"))
			.andExpect(jsonPath("$.walletId").exists())
			.andExpect(jsonPath("$.password").doesNotExist());

		String token = TestUsers.login(mvc, "maria@example.com", TestUsers.PASSWORD);
		mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Maria Silva"));
	}

	@Test
	void newWalletStartsAtZero() throws Exception {
		SignedUpUser maria = TestUsers.signUp(mvc, "Maria Silva", "52998224725", "maria@example.com");

		mvc.perform(get("/wallets/{id}", maria.walletId()).header(HttpHeaders.AUTHORIZATION, maria.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(0.00));
	}

	@Test
	void rejectsDuplicateCpfEvenWhenFormattedDifferently() throws Exception {
		createUser("Maria Silva", "529.982.247-25", "maria@example.com", TestUsers.PASSWORD)
			.andExpect(status().isCreated());

		createUser("Other Person", "52998224725", "other@example.com", TestUsers.PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A user with this CPF already exists"));
	}

	@Test
	void rejectsDuplicateEmailIgnoringCase() throws Exception {
		createUser("Maria Silva", "529.982.247-25", "maria@example.com", TestUsers.PASSWORD)
			.andExpect(status().isCreated());

		createUser("Other Person", "111.444.777-35", "MARIA@example.com", TestUsers.PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A user with this email already exists"));
	}

	@Test
	void rejectsInvalidFieldsWithOneErrorPerField() throws Exception {
		createUser("", "123.456.789-00", "not-an-email", "short")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid request"))
			.andExpect(jsonPath("$.errors.fullName").exists())
			.andExpect(jsonPath("$.errors.document").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.password").exists());
	}

	private ResultActions createUser(String fullName, String document, String email, String password)
			throws Exception {
		String json = """
				{"fullName": "%s", "document": "%s", "email": "%s", "password": "%s"}
				""".formatted(fullName, document, email, password);
		return mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
