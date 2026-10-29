package com.rodrigohen.wallet.transaction;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rodrigohen.wallet.auth.CurrentUserId;
import com.rodrigohen.wallet.common.ProblemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@Tag(name = "Transactions")
public class TransactionController {

	private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

	private static final String IDEMPOTENCY_KEY_DESCRIPTION = "Unique value per operation, e.g. a UUID. "
			+ "Retrying with the same key returns the original result instead of moving money again.";

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/wallets/{walletId}/deposits")
	@Operation(summary = "Deposit into one of your wallets")
	@ApiResponse(responseCode = "201", description = "Deposit made")
	@ApiResponse(responseCode = "200", description = "Retry of an earlier request: the original deposit is returned")
	@ProblemResponse.BadRequest
	@ProblemResponse.Unauthorized
	@ProblemResponse.WalletNotFound
	@ProblemResponse.BusinessRule
	public ResponseEntity<TransactionResponse> deposit(@CurrentUserId UUID userId, @PathVariable UUID walletId,
			@Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
			@RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
			@Valid @RequestBody DepositRequest request) {
		return respond(transactionService.deposit(userId, walletId, request.amount(), idempotencyKey));
	}

	@PostMapping("/transfers")
	@Operation(summary = "Transfer from one of your wallets to any wallet",
			description = "Safe under concurrent requests: both wallets are locked, so balances never go negative.")
	@ApiResponse(responseCode = "201", description = "Transfer made")
	@ApiResponse(responseCode = "200", description = "Retry of an earlier request: the original transfer is returned")
	@ProblemResponse.BadRequest
	@ProblemResponse.Unauthorized
	@ProblemResponse.WalletNotFound
	@ApiResponse(responseCode = "409", description = "The same Idempotency-Key is being processed right now")
	@ProblemResponse.BusinessRule
	public ResponseEntity<TransactionResponse> transfer(@CurrentUserId UUID userId,
			@Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
			@RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
			@Valid @RequestBody TransferRequest request) {
		return respond(transactionService.transfer(userId, request.sourceWalletId(), request.targetWalletId(),
				request.amount(), idempotencyKey));
	}

	@GetMapping("/wallets/{walletId}/transactions")
	@Operation(summary = "Statement of one of your wallets", description = "Newest first, paginated.")
	@ProblemResponse.BadRequest
	@ProblemResponse.Unauthorized
	@ProblemResponse.WalletNotFound
	public StatementPage statement(@CurrentUserId UUID userId, @PathVariable UUID walletId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return transactionService.statement(userId, walletId, page, size);
	}

	// 201 the first time; 200 with the original transaction when a retry replays the same Idempotency-Key
	private static ResponseEntity<TransactionResponse> respond(TransactionResult result) {
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(result.transaction());
	}

}
