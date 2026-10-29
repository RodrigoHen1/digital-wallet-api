package com.rodrigohen.wallet.transaction;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(

		@NotNull
		UUID sourceWalletId,

		@NotNull
		UUID targetWalletId,

		@NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2)
		BigDecimal amount) {
}
