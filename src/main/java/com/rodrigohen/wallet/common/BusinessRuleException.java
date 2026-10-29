package com.rodrigohen.wallet.common;

/**
 * The request is well formed but breaks a business rule, e.g. not enough balance for a transfer.
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}

}
