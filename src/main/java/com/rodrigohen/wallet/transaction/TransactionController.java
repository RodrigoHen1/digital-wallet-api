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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
public class TransactionController {

	private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/wallets/{walletId}/deposits")
	public ResponseEntity<TransactionResponse> deposit(@PathVariable UUID walletId,
			@RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
			@Valid @RequestBody DepositRequest request) {
		return respond(transactionService.deposit(walletId, request.amount(), idempotencyKey));
	}

	@PostMapping("/transfers")
	public ResponseEntity<TransactionResponse> transfer(
			@RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = 64) String idempotencyKey,
			@Valid @RequestBody TransferRequest request) {
		return respond(transactionService.transfer(request.sourceWalletId(), request.targetWalletId(),
				request.amount(), idempotencyKey));
	}

	@GetMapping("/wallets/{walletId}/transactions")
	public StatementPage statement(@PathVariable UUID walletId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return transactionService.statement(walletId, page, size);
	}

	// 201 the first time; 200 with the original transaction when a retry replays the same Idempotency-Key
	private static ResponseEntity<TransactionResponse> respond(TransactionResult result) {
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(result.transaction());
	}

}
