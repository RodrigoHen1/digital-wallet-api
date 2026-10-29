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

## API

| Method | Path            | Description                                  |
|--------|-----------------|----------------------------------------------|
| POST   | `/users`        | Create a user and their wallet (balance 0)   |
| GET    | `/users/{id}`   | Get a user                                   |
| GET    | `/wallets/{id}` | Get a wallet and its balance                 |

Create a user:

```bash
curl -i -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Maria Silva", "document": "529.982.247-25", "email": "maria@example.com"}'
```

```json
{
  "id": "6f1c2a9e-...",
  "fullName": "Maria Silva",
  "email": "maria@example.com",
  "walletId": "b3d7e4f0-...",
  "createdAt": "2026-10-29T14:03:12.481Z"
}
```

Errors use the [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format:

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/users",
  "errors": { "document": "invalid Brazilian individual taxpayer registry number (CPF)" }
}
```

### Design notes

- **CPF and email are normalized** (digits only, lowercase) before saving, so `529.982.247-25` and `52998224725` count as the same person.
- **Uniqueness is enforced twice:** the service checks first to return a clear message, and unique constraints in the database catch races between simultaneous requests.
- **User and wallet are created in one transaction**, so a user can never exist without a wallet.
- **Money is `NUMERIC(19,2)` / `BigDecimal`**, never `double`, and the database rejects negative balances with a `CHECK` constraint.
- **Wallets use optimistic locking (`@Version`)** to prepare for concurrent transfers.

## Running tests

```bash
./mvnw verify
```

Tests start a throwaway PostgreSQL container with Testcontainers, so Docker must be running.

## Roadmap

- [x] Project setup
- [x] Users and wallets
- [ ] Deposits and transfers (with concurrency safety and idempotency keys)
- [ ] Transaction statement
- [ ] Authentication with JWT
- [ ] OpenAPI / Swagger docs
- [ ] CI with GitHub Actions
- [ ] Dockerfile for the application
