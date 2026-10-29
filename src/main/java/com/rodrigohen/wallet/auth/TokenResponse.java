package com.rodrigohen.wallet.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
