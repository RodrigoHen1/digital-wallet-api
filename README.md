# Digital Wallet API

A REST API for a digital wallet: user accounts, balances, and money transfers between users.

> Work in progress — built step by step. See the roadmap below.

## Tech stack

- Java 25, Spring Boot 4
- PostgreSQL 17, Flyway migrations, Spring Data JPA
- Testcontainers for integration tests
- Docker Compose for local development

## Running locally

Requirements: JDK 25 and Docker Desktop (running).

```bash
./mvnw spring-boot:run
```

Spring Boot starts the PostgreSQL container from `compose.yaml` automatically.
Health check: http://localhost:8080/actuator/health

## Running tests

```bash
./mvnw verify
```

Tests start a throwaway PostgreSQL container with Testcontainers, so Docker must be running.

## Roadmap

- [x] Project setup
- [ ] Users and wallets
- [ ] Deposits and transfers (with concurrency safety and idempotency keys)
- [ ] Transaction statement
- [ ] Authentication with JWT
- [ ] OpenAPI / Swagger docs
- [ ] CI with GitHub Actions
- [ ] Dockerfile for the application
