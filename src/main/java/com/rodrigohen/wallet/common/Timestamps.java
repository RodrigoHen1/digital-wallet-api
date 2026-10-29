package com.rodrigohen.wallet.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Timestamps {

	private Timestamps() {
	}

	/**
	 * The current time at PostgreSQL's precision (microseconds), so a value returned before saving matches the value
	 * read back from the database later.
	 */
	public static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

}
