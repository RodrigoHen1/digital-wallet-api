package com.rodrigohen.wallet.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.rodrigohen.wallet.TestcontainersConfiguration;
import com.rodrigohen.wallet.common.BusinessRuleException;
import com.rodrigohen.wallet.user.CreateUserRequest;
import com.rodrigohen.wallet.user.UserResponse;
import com.rodrigohen.wallet.user.UserService;
import com.rodrigohen.wallet.wallet.WalletService;

/**
 * Fires many transfers at the same time against a real PostgreSQL to prove balances stay correct under load.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TransferConcurrencyTests {

	private static final int THREADS = 20;

	@Autowired
	private TransactionService transactionService;

	@Autowired
	private UserService userService;

	@Autowired
	private WalletService walletService;

	@Autowired
	private JdbcTemplate jdbc;

	private UserResponse maria;

	private UserResponse joao;

	@BeforeEach
	void setUp() {
		jdbc.execute("TRUNCATE transactions, wallets, users");
		maria = userService.create(new CreateUserRequest("Maria Silva", "52998224725", "maria@example.com", "secret-password"));
		joao = userService.create(new CreateUserRequest("Joao Souza", "11144477735", "joao@example.com", "secret-password"));
	}

	@Test
	void concurrentTransfersNeverOverspend() throws Exception {
		transactionService.deposit(maria.id(), maria.walletId(), new BigDecimal("100.00"), "dep-maria");

		// 20 transfers of 10.00 race for a balance of 100.00: exactly 10 may succeed
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			String key = "tr-" + i;
			tasks.add(() -> {
				try {
					transactionService.transfer(maria.id(), maria.walletId(), joao.walletId(),
							new BigDecimal("10.00"), key);
					return true;
				}
				catch (BusinessRuleException ex) {
					return false;
				}
			});
		}

		List<Boolean> results = runAllAtOnce(tasks);

		assertThat(results).filteredOn(succeeded -> succeeded).hasSize(10);
		assertThat(balance(maria)).isEqualByComparingTo("0.00");
		assertThat(balance(joao)).isEqualByComparingTo("100.00");
	}

	@Test
	void opposingTransfersDoNotDeadlock() throws Exception {
		transactionService.deposit(maria.id(), maria.walletId(), new BigDecimal("100.00"), "dep-maria");
		transactionService.deposit(joao.id(), joao.walletId(), new BigDecimal("100.00"), "dep-joao");

		// Half the threads send Maria -> Joao and half Joao -> Maria, all at once
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			boolean fromMaria = i % 2 == 0;
			String key = "tr-" + i;
			tasks.add(() -> {
				transactionService.transfer(fromMaria ? maria.id() : joao.id(),
						fromMaria ? maria.walletId() : joao.walletId(), fromMaria ? joao.walletId() : maria.walletId(),
						new BigDecimal("1.00"), key);
				return true;
			});
		}

		List<Boolean> results = runAllAtOnce(tasks);

		assertThat(results).hasSize(THREADS).containsOnly(true);
		assertThat(balance(maria)).isEqualByComparingTo("100.00");
		assertThat(balance(joao)).isEqualByComparingTo("100.00");
	}

	@Test
	void sameIdempotencyKeySentConcurrentlyMovesMoneyOnce() throws Exception {
		transactionService.deposit(maria.id(), maria.walletId(), new BigDecimal("100.00"), "dep-maria");

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			tasks.add(() -> {
				try {
					transactionService.transfer(maria.id(), maria.walletId(), joao.walletId(),
							new BigDecimal("10.00"), "same-key");
					return true;
				}
				catch (DataIntegrityViolationException ex) {
					// A duplicate that raced past the replay check is stopped by the unique constraint
					return false;
				}
			});
		}

		runAllAtOnce(tasks);

		assertThat(balance(maria)).isEqualByComparingTo("90.00");
		assertThat(balance(joao)).isEqualByComparingTo("10.00");
	}

	private BigDecimal balance(UserResponse owner) {
		return walletService.get(owner.walletId(), owner.id()).balance();
	}

	/**
	 * Starts every task at the same moment and waits for all of them, failing the test if any thread hangs.
	 */
	private static List<Boolean> runAllAtOnce(List<Callable<Boolean>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (Callable<Boolean> task : tasks) {
				futures.add(pool.submit(() -> {
					start.await();
					return task.call();
				}));
			}
			start.countDown();

			List<Boolean> results = new ArrayList<>();
			for (Future<Boolean> future : futures) {
				results.add(future.get(30, TimeUnit.SECONDS));
			}
			return results;
		}
	}

}
