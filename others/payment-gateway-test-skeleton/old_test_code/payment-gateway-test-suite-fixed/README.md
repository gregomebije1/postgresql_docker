# Payment Gateway Spring Boot Test Suite

Java 21 + Spring Boot sample implementing and testing:

- `POST /api/v1/payments`
- Stripe-style webhook verification
- HMAC-SHA256 signatures
- 300-second replay protection
- Redis idempotency
- PostgreSQL persistence
- Transactional outbox
- Unit tests with Mockito/JUnit
- HTTP integration tests
- Transaction rollback tests
- Testcontainers PostgreSQL + Redis

## Requirements

- JDK 21
- Maven 3.9+
- Docker Desktop / Docker Engine for Testcontainers tests

## Run unit tests

```bash
mvn -Dtest=WebhookSignatureVerifierTest,PaymentServiceUnitTest test
```

## Run Spring integration tests

Docker must be running:

```bash
mvn test
```

## Main flow

```text
Client
  |
  | POST /api/v1/payments
  | Idempotency-Key
  v
Spring Boot
  |
  +--> Redis idempotency lock
  |
  +--> PostgreSQL payment
  |
  +--> PostgreSQL outbox event
  |
  v
External provider / Kafka publisher

External provider
  |
  | webhook + timestamp + HMAC signature
  v
WebhookController
  |
  +--> verify timestamp <= 300 seconds
  +--> verify HMAC over timestamp + "." + raw body
  +--> update payment
```

## Important production notes

This is a runnable testing skeleton rather than a production payment processor.

For production, add:

- persistent idempotency records with completed response replay
- atomic Redis Lua state transitions
- unique database constraints for business keys
- transactional outbox publisher with Kafka acknowledgement
- dead-letter/retry handling
- webhook delivery retry state
- structured audit logging
- secret rotation / KMS or Vault
- constant-time comparisons
- strict JSON schema validation
- ISO 20022 XSD validation where applicable
- rate limiting / token bucket
- PCI-DSS network segmentation
- observability and security testing
- concurrency/chaos/load tests

The webhook sample deliberately verifies the raw request body before parsing it, which is important because re-serializing JSON can change the bytes covered by the signature.
