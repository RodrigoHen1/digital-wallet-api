package com.rodrigohen.wallet.transaction;

import java.util.List;

import org.springframework.data.domain.Page;

public record StatementPage(List<StatementEntry> entries, int page, int size, long totalEntries, int totalPages) {

	static StatementPage from(Page<StatementEntry> page) {
		return new StatementPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

}
