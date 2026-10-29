package com.rodrigohen.wallet.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String email, @NotBlank String password) {
}
