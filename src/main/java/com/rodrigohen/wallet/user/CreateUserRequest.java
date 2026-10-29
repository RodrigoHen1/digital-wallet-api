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
		String email) {
}
