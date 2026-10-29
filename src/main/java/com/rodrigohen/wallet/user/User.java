package com.rodrigohen.wallet.user;

import java.time.Instant;
import java.util.UUID;

import com.rodrigohen.wallet.common.Timestamps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	private UUID id;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String document;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected User() {
		// required by JPA
	}

	public User(String fullName, String document, String email, String passwordHash) {
		this.id = UUID.randomUUID();
		this.fullName = fullName;
		this.document = document;
		this.email = email;
		this.passwordHash = passwordHash;
		this.createdAt = Timestamps.now();
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getDocument() {
		return document;
	}

	public String getEmail() {
		return email;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
