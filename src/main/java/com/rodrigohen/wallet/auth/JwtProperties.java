package com.rodrigohen.wallet.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret base64-encoded HMAC key of at least 256 bits; if empty, a random key is generated at startup
 * @param ttl how long an access token stays valid
 */
@ConfigurationProperties("wallet.jwt")
public record JwtProperties(String secret, @DefaultValue("15m") Duration ttl) {
}
