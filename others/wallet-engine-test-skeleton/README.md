# Wallet Engine Test Skeleton

Runnable Spring Boot 3 / Java 21 test skeleton covering the requirements in the supplied e-wallet specification.

## Run

Prerequisites:

- Java 21+
- Maven 3.9+
- Docker, for the Redis Testcontainers test

Run the fast/unit/web/integration suite:

```bash
mvn test
```

Run the Redis integration test as well:

```bash
mvn -Dtest=RedisIdempotencyIntegrationTest test
```

## Test groups

- KYC tier/value limits
- wallet account provisioning
- funding intent
- payment webhook validation/replay protection
- P2P validation
- distributed idempotency semantics
- 50-thread debit race
- VAS hold/capture/release
- Temporal saga compensation ordering
- Temporal retry
- Resilience4j circuit breaker
- timeout to retry queue
- Kafka confirmation event
- Kafka DLQ
- traceparent propagation
- activity reconstruction
- AES-256 encryption at rest
- end-to-end orchestration ordering

The application code is intentionally small and deterministic: it supplies runnable seams for the tests rather than pretending to implement real banking/ledger/provider integrations.
