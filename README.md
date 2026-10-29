# Digital Wallet API

A REST API for a digital wallet: user accounts, deposits, transfers between users and a paginated statement. It is built to stay correct when many requests hit the same wallet at once.

> Work in progress — built step by step. See the roadmap below.

## Tech stack

- Java 25, Spring Boot 4
- Spring Security with JWT (OAuth2 resource server), BCrypt
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

To sign tokens with a fixed key (so they survive restarts), set `JWT_SECRET` to a base64 key of at least 32 bytes, e.g. `openssl rand -base64 32`. Without it, a random key is generated at startup.

## API

| Method | Path                            | Auth | Description                                         |
|--------|---------------------------------|------|-----------------------------------------------------|
| POST   | `/users`                        | —    | Sign up: create a user and their wallet (balance 0) |
| POST   | `/auth/token`                   | —    | Log in: exchange email + password for a token       |
| GET    | `/users/me`                     | ✔    | The logged-in user                                  |
| GET    | `/wallets/{id}`                 | ✔    | One of your wallets and its balance                 |
| POST   | `/wallets/{id}/deposits`        | ✔    | Add money to your wallet (requires `Idempotency-Key`) |
| POST   | `/transfers`                    | ✔    | Move money from your wallet to any wallet (requires `Idempotency-Key`) |
| GET    | `/wallets/{id}/transactions`    | ✔    | Your wallet's statement, newest first (`?page=0&size=20`, max 100) |

Endpoints marked ✔ need an `Authorization: Bearer <token>` header.

Sign up:

```bash
curl -i -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Maria Silva", "document": "529.982.247-25", "email": "maria@example.com", "password": "correct-horse-battery"}'
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

Log in:

```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@example.com", "password": "correct-horse-battery"}'
```

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresIn": 900 }
```

Transfer money:

```bash
curl -i -X POST http://localhost:8080/transfers \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 7c9e6679-7425-40de-944b-e07fc1f90ae7" \
  -d '{"sourceWalletId": "b3d7e4f0-...", "targetWalletId": "a1c2d3e4-...", "amount": 30.50}'
```

Statement of a wallet:

```json
{
  "entries": [
    { "transactionId": "...", "type": "TRANSFER", "direction": "OUT", "amount": 30.50,
      "counterpartyWalletId": "a1c2d3e4-...", "createdAt": "2026-10-29T15:12:40.120Z" },
    { "transactionId": "...", "type": "DEPOSIT", "direction": "IN", "amount": 100.00,
      "counterpartyWalletId": null, "createdAt": "2026-10-29T15:10:02.981Z" }
  ],
  "page": 0, "size": 20, "totalEntries": 2, "totalPages": 1
}
```

### Errors

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

| Status | When |
|--------|------|
| 400 | Invalid input, missing `Idempotency-Key` |
| 401 | Missing, invalid or expired token; wrong email or password |
| 404 | Unknown wallet, or a wallet that belongs to someone else |
| 409 | Duplicate CPF or email; a request with the same `Idempotency-Key` is still being processed |
| 422 | Insufficient balance, transfer to the same wallet, `Idempotency-Key` reused for a different request |

## Design notes

### Authentication and authorization

- **Stateless JWT bearer tokens** (HS256, 15 minutes), issued by `/auth/token` and validated by Spring Security's OAuth2 resource server. The token's subject is the user id; the server keeps no sessions.
- **Passwords are hashed with BCrypt** through Spring's delegating encoder (stored as `{bcrypt}...`), so the algorithm can be upgraded later without breaking existing passwords. Passwords must be 8–72 characters, because BCrypt ignores anything past 72 bytes.
- **Login doesn't reveal which emails exist:** a wrong email and a wrong password return the same message, and an unknown email still runs a password check so both take the same time.
- **Ownership is checked on every wallet operation.** You can only read, deposit into and transfer out of your own wallets; you can transfer *to* anyone.
- **Someone else's wallet returns 404, not 403**, exactly like a wallet that doesn't exist, so wallet ids can't be probed.
- **Idempotency keys are scoped per user**, so two users can never collide on the same key.

### Concurrency: no money is created or lost

Two transfers from the same wallet at the same moment could both read a balance of 100 and both spend it. To prevent that, a transfer locks both wallets with `SELECT ... FOR UPDATE` (pessimistic locking): the second transfer waits until the first commits, then sees the updated balance.

The wallets are always locked **in the same order (lowest id first)**. Without that, a transfer A→B and a transfer B→A running together could each hold one lock and wait for the other forever — a deadlock.

`TransferConcurrencyTests` proves both: 20 simultaneous transfers of 10.00 from a balance of 100.00 result in exactly 10 successes, and 20 opposing transfers between two wallets all complete without deadlock.

### Idempotency: a retry never charges twice

If a client's connection drops after sending a transfer, it cannot know whether the transfer happened. Every deposit and transfer therefore requires an `Idempotency-Key` header (any unique string, e.g. a UUID):

- **Same key, same request** → the original transaction is returned (`200 OK`), no money moves again.
- **Same key, different request** → `422`, because that is a client bug.
- **Same key sent twice at the same instant** → the database's unique constraint on (user, key) lets only one through; the whole second transaction rolls back.

### Data integrity

- **Money is `NUMERIC(19,2)` / `BigDecimal`**, never `double`. Amounts must be positive with at most 2 decimals.
- **The database enforces the rules too:** balances can't go negative, amounts must be positive, a deposit has no source wallet and a transfer can't target its own source (`CHECK` constraints). Bugs in the Java code can't corrupt the data.
- **Transactions are append-only.** Rows are inserted, never updated, so the statement is a reliable history.
- **CPF and email are normalized** (digits only, lowercase) before saving, so `529.982.247-25` and `52998224725` count as the same person. Uniqueness is checked in the service for a clear message and by unique constraints for races.
- **Timestamps are truncated to microseconds**, PostgreSQL's precision, so a response returned before saving matches what is read back later.

## Running tests

```bash
./mvnw verify
```

Tests start a throwaway PostgreSQL container with Testcontainers, so Docker must be running. The suite has unit tests (Mockito, no database), API integration tests (MockMvc against a real database) and concurrency tests.

## Roadmap

- [x] Project setup
- [x] Users and wallets
- [x] Deposits and transfers (with concurrency safety and idempotency keys)
- [x] Transaction statement
- [x] Authentication with JWT
- [ ] OpenAPI / Swagger docs
- [ ] CI with GitHub Actions
- [ ] Dockerfile for the application
