package com.rodrigohen.wallet;

import org.springframework.boot.SpringApplication;

public class TestDigitalWalletApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(DigitalWalletApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
