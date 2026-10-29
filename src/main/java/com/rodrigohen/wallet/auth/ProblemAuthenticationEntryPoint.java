package com.rodrigohen.wallet.auth;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers a missing or invalid token with 401 in the same problem-detail format as the rest of the API, keeping
 * the standard {@code WWW-Authenticate: Bearer} header.
 */
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		bearer.commence(request, response, authException);
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("""
				{"type":"about:blank","title":"Unauthorized","status":401,\
				"detail":"A valid bearer token is required","instance":"%s"}"""
			.formatted(request.getRequestURI().replace("\"", "")));
	}

}
