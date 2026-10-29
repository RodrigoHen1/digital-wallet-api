package com.rodrigohen.wallet.user;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.rodrigohen.wallet.TestcontainersConfiguration;
import com.rodrigohen.wallet.wallet.WalletRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiIntegrationTests {

	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private WalletRepository wallets;

	@BeforeEach
	void cleanDatabase() {
		wallets.deleteAll();
		users.deleteAll();
	}

	@Test
	void createsUserWithEmptyWallet() throws Exception {
		String body = createUser("Maria Silva", "529.982.247-25", "Maria@Example.com")
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern("/users/" + UUID_PATTERN)))
			.andExpect(jsonPath("$.fullName").value("Maria Silva"))
			.andExpect(jsonPath("$.email").value("maria@example.com"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		String walletId = JsonPath.read(body, "$.walletId");
		mvc.perform(get("/wallets/{id}", walletId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.balance").value(0.00));
	}

	@Test
	void returnsCreatedUserById() throws Exception {
		String body = createUser("Joao Souza", "11144477735", "joao@example.com").andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(body, "$.id");

		mvc.perform(get("/users/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.fullName").value("Joao Souza"));
	}

	@Test
	void rejectsDuplicateCpfEvenWhenFormattedDifferently() throws Exception {
		createUser("Maria Silva", "529.982.247-25", "maria@example.com").andExpect(status().isCreated());

		createUser("Other Person", "52998224725", "other@example.com")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A user with this CPF already exists"));
	}

	@Test
	void rejectsDuplicateEmailIgnoringCase() throws Exception {
		createUser("Maria Silva", "529.982.247-25", "maria@example.com").andExpect(status().isCreated());

		createUser("Other Person", "111.444.777-35", "MARIA@example.com")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("A user with this email already exists"));
	}

	@Test
	void rejectsInvalidFieldsWithOneErrorPerField() throws Exception {
		createUser("", "123.456.789-00", "not-an-email")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid request"))
			.andExpect(jsonPath("$.errors.fullName").exists())
			.andExpect(jsonPath("$.errors.document").exists())
			.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void returnsNotFoundForUnknownIds() throws Exception {
		mvc.perform(get("/users/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
		mvc.perform(get("/wallets/{id}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Resource not found"));
	}

	private ResultActions createUser(String fullName, String document, String email) throws Exception {
		String json = """
				{"fullName": "%s", "document": "%s", "email": "%s"}
				""".formatted(fullName, document, email);
		return mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json));
	}

}
