# Multi-Tenant Double-Entry Ledger — Spring Boot Test Skeleton

This project is a runnable starting point for the unit, API, integration,
and distributed tests described in the ledger specification.

## Requirements

- JDK 21+
- Maven 3.9+
- Docker
- Docker daemon available to Testcontainers

## Run unit tests

```bash
mvn test
```

## Run integration/distributed tests

```bash
mvn -Pintegration test
```

The distributed tests start a 3-node CockroachDB Testcontainers cluster.

## Important

This is intentionally a **test skeleton**, not a completed financial
implementation.

The unit and controller tests are runnable immediately. The distributed
tests establish the Testcontainers topology and test structure, but the
placeholder sections marked "Replace..." must be connected to the real
LedgerService/repositories before they can prove production invariants.

In particular, production code should add:

1. Immutable journal persistence.
2. Per-transaction zero-sum enforcement.
3. Tenant authentication and row-level isolation.
4. Hold/available/locked balance operations.
5. CockroachDB serializable transaction retry handling.
6. Real application routing to distinct database nodes.
7. Database-level append-only protections.
8. HMAC/SHA-256 chain verification.
9. Quorum-loss write failure handling.
10. Read-after-write verification across nodes.

## Test layout

- `core/*Test.java` — pure in-memory ledger rules.
- `api/*Test.java` — MockMvc endpoint contract.
- `distributed/*IT.java` — multi-node CockroachDB/Testcontainers scenarios.

## Recommended production invariant

Do not audit only:

```sql
SELECT SUM(amount) FROM journal_entries;
```

Store entry direction and audit:

```sql
SELECT COALESCE(
    SUM(
        CASE
            WHEN entry_type = 'DEBIT' THEN amount
            WHEN entry_type = 'CREDIT' THEN -amount
            ELSE 0
        END
    ),
    0
)
FROM journal_entries;
```

Also audit the same invariant independently for every transaction.

## Note on this skeleton's Spring Boot version

This project targets Spring Boot 3.5.6. `LedgerControllerTest` uses
`@MockitoBean` (Spring Framework 6.2+/Boot 3.4+) rather than the older
`@MockBean`, since `@MockBean` is deprecated as of Boot 3.4 and removed
entirely in Boot 4.0 — using `@MockitoBean` now means this test class
won't need touching if you later upgrade to Boot 4.
