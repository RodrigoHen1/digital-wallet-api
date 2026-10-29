package com.rodrigohen.wallet.user;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

		@NotBlank @Size(max = 120)
		String fullName,

		@NotBlank @CPF
		String document,

		@NotBlank @Email @Size(max = 255)
		String email,

		// BCrypt only uses the first 72 bytes of a password, so longer ones are rejected rather than silently cut
		@NotBlank @Size(min = 8, max = 72)
		String password) {

	// Keeps the password out of logs if the request is ever printed
	@Override
	public String toString() {
		return "CreateUserRequest[fullName=" + fullName + ", email=" + email + "]";
	}

}
