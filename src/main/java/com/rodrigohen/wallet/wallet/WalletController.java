package com.rodrigohen.wallet.wallet;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rodrigohen.wallet.auth.CurrentUserId;
import com.rodrigohen.wallet.common.ProblemResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Wallets")
public class WalletController {

	private final WalletService walletService;

	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one of your wallets and its balance")
	@ProblemResponse.Unauthorized
	@ProblemResponse.WalletNotFound
	public WalletResponse get(@PathVariable UUID id, @CurrentUserId UUID userId) {
		return walletService.get(id, userId);
	}

}
