# Monzo Backend Engineer (Backend III) Prep Guide

Prepared 20 November 2026. Sources: Monzo's own published interview guide (Suhail Patel, 1 March 2025, fetched and read for this guide) and public candidate reports. URLs outside Monzo's page are given from memory: if a deep link has moved, search the title and use the project home page.

## 1. What Monzo says it tests

Source: https://monzo.com/blog/demystifying-the-backend-engineering-interview-process (read this in full, twice).

After a recruiter call there are four stages, all run by working Monzo engineers or engineering managers:

1. **Initial call (1 hour).** A deep dive into a recent project you had real input on: how it was built, data structures, language choices, performance and reliability trade-offs, security, what you would do differently. Expect questions about a big technical challenge, customer-facing work and cross-functional teamwork. No whiteboarding and no code shown.
2. **Take-home or pair coding (your choice).**
   - Take-home: build a small program in any language, submit it with a README (structure, how to run, trade-offs, what you would improve), then walk through it on a review call. Extra features earn no bonus; it must do what the task sheet says.
   - Pair coding: 45 minutes, implement functions to satisfy a given interface, with unit tests provided. Offered in Go, Python and Java. Explicitly not LeetCode, not a speed test, and not a test of language trivia. Docs and web search are allowed.
3. **Systems design.** A hypothetical problem that is not Monzo-specific, drawn in a tool like Excalidraw (https://excalidraw.com). You pick databases and queues, state trade-offs, and respond as the interviewer adds new information. Priority: a shared understanding of information flow and the performance, consistency and availability trade-offs. They recommend Designing Data-Intensive Applications, especially Part II, and warn against dropping buzzwords you cannot reason about. If you name an AWS or GCP service, expect questions on how it works underneath.
4. **Behavioural (1 hour, two interviewers).** Four areas: communication, learning, working in a team, delivering projects. Distinguish what you personally did from what the team did.

Other rules from Monzo: it is fine to say 'I don't know' and reason from first principles; LLMs are not allowed during interviews (fine for preparing); you are interviewing them too.

Senior/Staff variant (same process, different bar): https://monzo.com/blog/demystifying-the-senior-staff-engineering-interview-process Monzo on using AI to prepare: https://monzo.com/blog/the-ultimate-guide-on-using-ai-to-ace-an-interview-at-monzo

## 2. What candidates report (an honest note on 'every question')

No complete or official list of Backend III questions exists, and anyone claiming one is guessing. Monzo itself says there are no gotchas. What public reports (mostly Glassdoor, https://www.glassdoor.co.uk/Interview/Monzo-Bank-Backend-Engineer-Interview-Questions-EI\_IE1557148.0,10\_KO11,27.htm) show:

- The process matches the blog post closely, and reviewers say it is streamlined, usually 2 to 4 weeks, with fast recruiter replies.
- The engineer call dwells on previous experience, especially designing distributed systems, and which projects challenged you.
- Take-home tasks reported over the years include a web crawler in Go (2019) and a Go exercise (2017). The review call probes your decisions (one reviewer was challenged on using recursion), so be ready to defend and improve every choice.
- Individual questions reported are basic: what an HTTP request is, what recursion is, thread vs process. Expect fundamentals questions inside the conversation rather than a quiz.
- Mixed experiences exist: some call it a great process, one 2026 reviewer called an interview unstructured, and at least one candidate was rejected at the take-home review. Treat all of this as a pattern, not a script.

Backend Engineer III job listings map to Monzo's L40 level (L50 senior, L60 staff). The interview covers the whole progression framework and levels you from performance, so prepare to the same standard regardless of title.

### Practice questions likely given Monzo's stated emphasis (my inference, not reported Monzo questions)

- Design an idempotent payments API that survives client retries and server crashes.
- Two requests update the same account balance at once. What can go wrong and how do you stop it?
- Design a webhook delivery system (retries, ordering, dead letters).
- Design a rate limiter, a job scheduler, a notification service, a transaction history feed.
- You publish an event after a DB commit and the process dies in between. What now?
- Put a cache in front of this read path. What breaks?
- Your Kafka consumer processes a message twice. What happens to the balance?

### Preparation matrix

| Stage | Prepare | Practice |
| --- | --- | --- |
| Initial call | One project you can discuss for an hour: requirements, design, trade-offs, failures, metrics, hindsight | Write a one-page design doc for your gateway and say it aloud |
| Coding | Plain Java (no Spring): collections, records, interfaces, exceptions, JUnit 5, BigDecimal or long for money, concurrency basics | Implement interfaces against provided tests in 45 minutes, narrating trade-offs |
| Systems design | Sections 3 and 4 below | Two designs a week on Excalidraw, aloud, with a friend |
| Behavioural | 6 stories (conflict, delivery under ambiguity, mistake, mentoring, influencing, learning) in STAR form, each with your personal role | Record yourself |

## 3. Distributed systems curriculum

Each topic: what it is, where it shows up in your ledger, wallet or gateway, trade-offs, and free resources. Core free sources used throughout:

- MIT 6.5840 (formerly 6.824) Distributed Systems, lectures, notes and Go labs: https://pdos.csail.mit.edu/6.824/
- Martin Kleppmann's distributed systems lecture series and notes (Cambridge, free on YouTube; search 'Kleppmann Distributed Systems lecture series')
- Jepsen analyses and consistency map: https://jepsen.io/analyses and https://jepsen.io/consistency
- System Design Primer: https://github.com/donnemartin/system-design-primer
- Google SRE book (free): https://sre.google/sre-book/table-of-contents/
- AWS Builders' Library (free, practical): https://aws.amazon.com/builders-library/
- Book (paid, but Monzo recommends it): Designing Data-Intensive Applications, Kleppmann, https://dataintensive.net/ . Read Part II (replication, partitioning, transactions, trouble with distributed systems, consistency and consensus), then Part III.

### 3.1 Partial failure, timeouts, retries

- A timeout means the outcome is unknown, not failed.
- Gateway: the PSP (payment provider) call times out. Mark the payment UNKNOWN, query the provider by your idempotency reference, and reconcile later. Never blindly retry a call that is not idempotent.
- Trade-off: retry immediately (low latency, duplicate risk) vs record unknown and reconcile (slower, safe).
- Learn: timeouts and deadlines propagated across calls, exponential backoff with jitter, retry budgets, cascading failure.
- Resources: https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/ ; https://sre.google/sre-book/addressing-cascading-failures/

### 3.2 Idempotency and delivery semantics

- At-most-once, at-least-once, 'exactly-once' (which in practice means at-least-once delivery plus idempotent processing).
- Gateway: Idempotency-Key header, request fingerprint, stored response. Ledger: unique idempotency key per transaction. Consumers: your inbox table dedupes by message id.
- Trade-offs: key store in Postgres (durable, transactional with the work) vs Redis (fast, but a separate system that can lose or diverge from the DB). Safest: Postgres is the source of truth, Redis only accelerates.
- Resources: https://docs.stripe.com/api/idempotent\_requests ; https://brandur.org/idempotency-keys ; https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-apis/

### 3.3 Clocks and ordering

- Wall clocks drift and jump, so never order ledger entries or decide winners by timestamp. Use per-account sequence numbers or database sequences. Know Lamport and vector clocks, happens-before.
- Where: ledger entry ordering, event ordering in Kafka (order exists only within a partition), webhook events arriving out of order (store provider event time but compare state-machine transitions, not clocks).
- Resources: DDIA chapter 8; Kleppmann lectures.

### 3.4 Consistency models

- Linearizable, serializable, causal, read-your-writes, monotonic reads, eventual. Know the difference between serializable (transactions) and linearizable (single-object recency).
- Where: balance read right after a payment (read-your-writes), reading from a replica (stale), showing 'available balance'.
- Trade-off: strong consistency costs latency and availability. Money movement on the write path needs it; feeds and history can be stale.
- Resources: https://jepsen.io/consistency ; CAP and PACELC (read Brewer 'CAP twelve years later' and Abadi's PACELC paper).

### 3.5 Replication and partitioning

- Leader-follower, multi-leader, leaderless (quorums). Replication lag. Partition by account id, hot keys, consistent hashing, resharding.
- Where: Postgres streaming replication for the ledger, Kafka partitions keyed by account id to keep per-account order, Cassandra (Monzo's main store) partition keys.
- Trade-offs: replicas scale reads but serve stale data; sharding scales writes but makes cross-shard transfers a distributed transaction problem. A hot account (a settlement or fees account touched by every payment) becomes a bottleneck: split it into N sub-accounts and sum them.
- Resources: Amazon Dynamo paper https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf ; Cassandra docs https://cassandra.apache.org/doc/latest/ (data modelling: design tables per query, partition and clustering keys, tunable consistency, lightweight transactions, tombstones).

### 3.5b Cassandra specifically (Monzo's main store, listed in its job posts)

Learn query-first data modelling, no joins, denormalisation, partition sizing, tunable consistency (ONE, QUORUM, ALL), lightweight transactions (compare-and-set, slow), and why it favours availability. Interview angle: why would a bank choose it, and how do you protect an invariant like no double spend when there are no multi-row transactions? (Answer sketch: serialise per account through a single writer or LWT, and append immutable records.)

### 3.6 Transactions across services

- ACID, isolation anomalies (dirty read, non-repeatable read, lost update, write skew, phantom). Two-phase commit and why it is avoided across services. Sagas (orchestrated vs choreographed), compensating actions, TCC.
- Where: wallet top-up = gateway payment succeeds, then ledger credit; if the ledger step fails you need a retry or a refund. Transfer between wallets on different shards.
- Trade-offs: orchestration (one place to see the flow, central coupling) vs choreography (loose coupling, hard to trace).
- Resources: https://microservices.io/patterns/data/saga.html ; Pat Helland, 'Life beyond Distributed Transactions' (ACM Queue); https://microservices.io/patterns/data/transactional-outbox.html ; Debezium outbox router https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html

### 3.7 Transactional outbox and inbox

- You already have both. Outbox: write the business row and the event row in one DB transaction; a publisher relays events to Kafka. Inbox: consumer records message ids to ignore duplicates.
- Alternatives to polling the outbox: change data capture (Debezium) reading the WAL, Postgres LISTEN/NOTIFY as a wake-up hint, or listen to the logical replication stream directly.
- Trade-offs: polling is simple but adds latency and has a visibility-ordering catch (see section 5); CDC is lower latency and ordered but adds infrastructure and operational burden.

### 3.8 Consensus and locks

- Raft and Paxos, leader election, quorum, split brain. Distributed locks and fencing tokens.
- Where: scheduled jobs that must run once (outbox publisher, settlement), leader election in a multi-instance gateway.
- Trade-offs: DB row lock or advisory lock (simple, tied to your DB), Redis lock (can fail under pauses and failover, see Kleppmann), etcd or ZooKeeper (correct but another system). Prefer optimistic concurrency or FOR UPDATE SKIP LOCKED over distributed locks whenever possible.
- Resources: https://raft.github.io/ ; http://thesecretlivesofdata.com/raft/ ; https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html

### 3.9 Messaging and streaming (Kafka)

- Topics, partitions, keys and ordering, consumer groups, offsets and commits, rebalances, retention, compaction, dead-letter topics, schema evolution, producer idempotence and transactions.
- Where: ledger emits TransactionPosted events, wallet consumes them to update read models, gateway emits PaymentSucceeded.
- Trade-offs: key by account id for order (hot keys hurt) vs random for throughput; commit offset after processing (at-least-once, needs idempotent consumers) vs before (at-most-once, can lose events).
- Resources: https://kafka.apache.org/documentation/ (Design chapter: delivery semantics, replication) ; https://developer.confluent.io/

### 3.10 Event sourcing and CQRS

- Store the sequence of facts, derive state. A ledger is naturally event-sourced. CQRS splits write model and read models.
- Trade-offs: perfect audit and replay vs complexity, schema evolution and eventual consistency of read models. For your projects: ledger entries are the events; balances and statements are read models.
- Resources: https://martinfowler.com/eaaDev/EventSourcing.html ; https://martinfowler.com/bliki/CQRS.html

### 3.11 Caching and Redis

- Cache-aside, write-through, TTLs, invalidation, stampedes. Never cache a balance you use to authorise spending without a version or lock check.
- Where: gateway rate limiting and idempotency fast path, session and token data, read-only reference data.
- Resources: https://redis.io/docs/latest/ ; 'Scaling Memcache at Facebook' paper.

### 3.12 Rate limiting, backpressure, load shedding

- Token bucket, leaky bucket, sliding window; per-client limits; return 429 with Retry-After; shed load rather than queue unboundedly.
- Resources: https://stripe.com/blog/rate-limiters

### 3.13 Resilience patterns

- Circuit breaker, bulkhead, timeout, retry, fallback, graceful degradation, health checks, readiness vs liveness.
- Resources: https://martinfowler.com/bliki/CircuitBreaker.html ; Resilience4j docs (Java) https://resilience4j.readme.io/docs

### 3.14 Observability

- Structured logs with correlation ids, RED and USE metrics, distributed tracing, SLIs and SLOs, alerting on symptoms.
- Resources: https://opentelemetry.io/docs/concepts/ ; Google SRE book chapters on monitoring and SLOs.

### 3.15 Money-specific correctness

- Integers in minor units (never float or double), one currency per entry, rounding rules, reconciliation against external statements, immutability and audit, reversals instead of deletions, GDPR vs immutable data (store PII separately and delete or crypto-shred it).
- Resources: https://martinfowler.com/eaaCatalog/money.html ; Square 'Books' ledger post https://developer.squareup.com/blog/books-an-immutable-double-entry-accounting-database-service/ ; Modern Treasury 'Accounting for Developers' https://www.moderntreasury.com/journal/accounting-for-developers-part-i ; TigerBeetle docs and design talks https://docs.tigerbeetle.com/

### 3.16 Testing distributed code

- Concurrency tests (N threads hitting the same account then asserting the ledger sums to zero), property-based tests (jqwik for Java, rapid for Go), Testcontainers with real Postgres and Kafka, fault injection (kill the process between commit and publish), and read how Jepsen finds bugs.
- Resources: https://java.testcontainers.org/ ; https://jepsen.io/

### 3.17 Monzo's own engineering material (read these)

- Monzo engineering and technology blog: https://monzo.com/blog/topic/technology
- Suhail Patel's posts and talks: https://monzo.com/blog/author/suhail-patel (search YouTube and InfoQ for his QCon talks on Monzo's platform)
- Matt Heath's talk 'Building a Modern Bank Backend' (Go microservices, Cassandra, Kubernetes): search the title; an abstract is at https://www.imperial.ac.uk/computing/industry/aci/2016-17/monzo

## 4. The three projects: design, where concepts apply, trade-offs

### 4.1 Ledger (the source of truth for money)

**Core model.** Accounts, transactions (journal entries) and entries (postings). Each entry has an account id, a signed amount in minor units (pence) and a currency. Invariant: the entries of every transaction sum to zero per currency. Entries are append-only; mistakes are fixed by reversing transactions.

**Concepts used:** idempotency (unique key per transaction), isolation and locking (3.6), ordering (per-account sequence), outbox (events), partitioning (by account), audit and immutability (3.15), consistency (3.4).

**Key design decisions and alternatives**

| Decision | Option A | Option B | Notes |
| --- | --- | --- | --- |
| Balance | Derive by summing entries | Maintain a balance row updated in the same transaction | Sum is always correct but slows with history; balance row is fast but must be updated atomically and can be rebuilt from entries. Common answer: both, with the balance row as a cache you can verify. |
| Concurrency | Pessimistic: SELECT ... FOR UPDATE on account rows (lock in a fixed order to avoid deadlock) | Optimistic: version column, retry on conflict; or SERIALIZABLE with retry | Pessimistic is simpler under high contention; optimistic is better when conflicts are rare. |
| Overdraft check | Read balance, then write (race) | One atomic UPDATE ... WHERE balance >= amount | Always do the check and the change in one statement or under a lock. |
| Pending money | Separate holds table (authorisations) | Pending entries in the same table with a status | Holds are easy to expire; available = balance minus active holds. |
| Storage | Single Postgres primary | Sharded Postgres or Cassandra | Single node is simplest and correct. Sharding forces cross-shard transfers into sagas or a per-account single-writer design. |
| Specialised engine | Build on Postgres | TigerBeetle | Study TigerBeetle for ideas (fixed-size records, two-phase transfers, deterministic testing). |
| Hot accounts | One row | Split into N sub-accounts | A fees or settlement account touched by every payment serialises everything. |

**Failure scenarios to rehearse:** duplicate request, crash after debit before credit (impossible if both are in one transaction; possible across shards), crash after commit before event publish (outbox), consumer processes an event twice, two transfers deadlock, replica lag causes a wrong balance read.

### 4.2 Wallet (user-facing product on top of the ledger)

**Model.** A wallet maps a user to one or more ledger accounts (main, pots, per-currency). Operations: top-up, pay, transfer, withdraw, hold then capture or release, refund. Shows available balance and ledger balance.

**Concepts used:** sagas (top-up spans gateway and ledger), idempotency, holds and expiry, read models (CQRS), rate limiting and limits, caching (carefully), consistency (read-your-writes after a payment).

**Decisions and alternatives**

- Wallet owns the balance vs the ledger owns it: let the ledger be the single source of truth; the wallet keeps a read model updated from ledger events and falls back to the ledger for authorisation decisions.
- Synchronous call to the ledger vs async via events: payments need a synchronous, strongly consistent debit; notifications, statements and analytics use events.
- Holds: authorise (create hold), capture (convert to posted entry), release or expire. Trade-off between a dedicated holds table and pending entries.
- Limits (daily spend, velocity): enforce inside the same transaction as the debit when they must be exact, or in Redis when approximate is acceptable.
- Multi-currency: separate accounts per currency; FX is a transaction between currency accounts with a rate and fees; never mix currencies in one entry.

### 4.3 Payment gateway (your existing project)

**Model.** Accept payment requests, make them idempotent, talk to external providers, track a payment state machine, emit events, handle provider webhooks, reconcile.

**Concepts used:** idempotency keys with request fingerprinting (same key plus different body returns an error), outbox and inbox, timeouts and unknown outcomes (3.1), circuit breakers and bulkheads, rate limiting, webhook signature verification (HMAC) and dedupe by event id, out-of-order events, reconciliation, JWT auth, multi-tenancy (tenant\_id).

**State machine (example):** CREATED then AUTHORISED then CAPTURED then SETTLED, with branches FAILED, VOIDED, REFUNDED, and UNKNOWN for ambiguous timeouts. Enforce legal transitions in code and with a CHECK constraint or a transitions table.

**Decisions and alternatives**

- Sync vs async API: return the final result when fast, or 202 Accepted plus a status endpoint and webhooks when providers are slow.
- Idempotency store: Postgres row inserted first with status IN\_PROGRESS (unique key), then COMPLETED with the stored response; Redis only as a fast path.
- Outbox relay: polling (simple), CDC with Debezium (ordered and low latency, more infrastructure).
- Provider failover: circuit breaker per provider, route to a secondary, accept that a payment sent to provider A must not be retried on provider B without knowing A's outcome.
- Orchestration vs choreography for gateway to ledger to notification flows.
- Reconciliation: nightly job compares provider settlement files against your payments and ledger and opens exceptions for differences. This is where real bugs are caught.

## 5. PostgreSQL you need, with ledger, wallet and gateway examples

### 5.1 Syllabus (tick each off by building something with it)

- **Types:** `bigint` minor units for money (never float; avoid the `money` type), `numeric` only for rates, `timestamptz` (never plain timestamp), `uuid` with `gen_random_uuid()`, `jsonb` for payloads, `text` with CHECK constraints instead of enums when states may change.
- **Constraints as your safety net:** PRIMARY KEY, FOREIGN KEY, UNIQUE (idempotency), CHECK (amount > 0, valid states), NOT NULL, EXCLUDE, deferrable constraints and constraint triggers.
- **Writes:** `INSERT ... ON CONFLICT DO NOTHING / DO UPDATE`, `RETURNING`, `UPDATE ... WHERE` as an atomic guard, CTEs that write, `MERGE` (Postgres 15+).
- **Reads:** joins, `GROUP BY`, `FILTER`, window functions (running balance, row\_number, lag), CTEs, `LATERAL`, keyset pagination (never OFFSET on big tables).
- **Transactions and MVCC:** snapshots, Read Committed vs Repeatable Read vs Serializable, what each prevents, serialization failures (SQLSTATE 40001) and deadlocks (40P01) and why you must retry them.
- **Locking:** `FOR UPDATE`, `FOR NO KEY UPDATE`, `FOR SHARE`, `NOWAIT`, `SKIP LOCKED`, advisory locks, lock ordering, `pg_locks` and `pg_stat_activity`.
- **Indexes and plans:** B-tree, partial, covering (`INCLUDE`), GIN for jsonb, BRIN for time series, multi-column order, `EXPLAIN (ANALYZE, BUFFERS)`, `pg_stat_statements`.
- **Operations:** VACUUM and bloat, WAL, streaming and logical replication, replication lag, PgBouncer, partitioning by time, backups and point-in-time recovery, zero-downtime migrations (expand then contract) with Flyway.
- **Triggers and security:** triggers for immutability, row-level security for multi-tenancy, roles with least privilege (the app role cannot UPDATE or DELETE ledger\_entry).

Free resources:

- Official manual (concurrency control chapter 13 is mandatory): https://www.postgresql.org/docs/current/mvcc.html ; isolation levels https://www.postgresql.org/docs/current/transaction-iso.html ; explicit locking https://www.postgresql.org/docs/current/explicit-locking.html
- The Internals of PostgreSQL (free book): https://www.interdb.jp/pg/
- Use The Index, Luke: https://use-the-index-luke.com/
- PG Exercises (SQL drills): https://pgexercises.com/
- Postgres wiki, 'Don't Do This': https://wiki.postgresql.org/wiki/Don%27t\_Do\_This
- Hermitage (what each database really does at each isolation level, by Kleppmann): https://github.com/ept/hermitage
- Brandur's Postgres and idempotency posts: https://brandur.org/

### 5.2 Ledger schema

```sql
CREATE TABLE account (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    currency      char(3) NOT NULL,
    allow_negative boolean NOT NULL DEFAULT false,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ledger_transaction (
    id               uuid PRIMARY KEY,
    idempotency_key  text NOT NULL UNIQUE,
    description      text,
    posted_at        timestamptz NOT NULL DEFAULT now()
);

-- Signed amounts in minor units: credit positive, debit negative.
CREATE TABLE ledger_entry (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id uuid   NOT NULL REFERENCES ledger_transaction(id),
    account_id     uuid   NOT NULL REFERENCES account(id),
    amount         bigint NOT NULL CHECK (amount <> 0),
    currency       char(3) NOT NULL,
    created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ledger_entry_account_idx ON ledger_entry (account_id, id);
CREATE INDEX ledger_entry_tx_idx ON ledger_entry (transaction_id);

-- Cached balance; can always be rebuilt from entries and verified.
CREATE TABLE account_balance (
    account_id uuid PRIMARY KEY REFERENCES account(id),
    balance    bigint NOT NULL DEFAULT 0,
    version    bigint NOT NULL DEFAULT 0
);
```

Enforce that every transaction balances to zero per currency, checked at commit time:

```sql
CREATE FUNCTION assert_balanced() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM ledger_entry
        WHERE transaction_id = NEW.transaction_id
        GROUP BY currency HAVING SUM(amount) <> 0
    ) THEN
        RAISE EXCEPTION 'transaction % does not balance', NEW.transaction_id;
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER ledger_entry_balanced
    AFTER INSERT ON ledger_entry
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION assert_balanced();
```

Make entries immutable:

```sql
CREATE FUNCTION forbid_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'ledger rows are immutable'; END $$;

CREATE TRIGGER ledger_entry_immutable BEFORE UPDATE OR DELETE ON ledger_entry
    FOR EACH ROW EXECUTE FUNCTION forbid_change();
```

Verify the cache against the source of truth (run as a scheduled check, alert on any rows):

```sql
SELECT b.account_id, b.balance AS cached, COALESCE(SUM(e.amount), 0) AS actual
FROM account_balance b
LEFT JOIN ledger_entry e ON e.account_id = b.account_id
GROUP BY b.account_id, b.balance
HAVING b.balance <> COALESCE(SUM(e.amount), 0);
```

### 5.3 Idempotent, deadlock-safe, overdraft-safe transfer

```sql
CREATE OR REPLACE FUNCTION post_transfer(
    p_key text, p_from uuid, p_to uuid, p_amount bigint, p_currency char(3)
) RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
    v_tx uuid := gen_random_uuid();
    v_existing uuid;
BEGIN
    IF p_amount <= 0 THEN RAISE EXCEPTION 'amount must be positive'; END IF;
    IF p_from = p_to THEN RAISE EXCEPTION 'same account'; END IF;

    -- 1. Idempotency: first writer wins; a concurrent duplicate waits for it.
    INSERT INTO ledger_transaction (id, idempotency_key, description)
    VALUES (v_tx, p_key, 'transfer')
    ON CONFLICT (idempotency_key) DO NOTHING;
    IF NOT FOUND THEN
        SELECT id INTO v_existing FROM ledger_transaction WHERE idempotency_key = p_key;
        RETURN v_existing;           -- replay the original result
    END IF;

    -- 2. Lock both balance rows in a fixed order so opposite transfers cannot deadlock.
    PERFORM 1 FROM account_balance WHERE account_id IN (p_from, p_to)
        ORDER BY account_id FOR UPDATE;

    -- 3. Check and debit in one statement.
    UPDATE account_balance SET balance = balance - p_amount, version = version + 1
        WHERE account_id = p_from AND balance >= p_amount;
    IF NOT FOUND THEN RAISE EXCEPTION 'insufficient funds'; END IF;
    UPDATE account_balance SET balance = balance + p_amount, version = version + 1
        WHERE account_id = p_to;

    -- 4. Append the two entries; the deferred trigger checks they sum to zero.
    INSERT INTO ledger_entry (transaction_id, account_id, amount, currency) VALUES
        (v_tx, p_from, -p_amount, p_currency),
        (v_tx, p_to,    p_amount, p_currency);
    RETURN v_tx;
END $$;
```

Things this deliberately leaves for you to discuss and improve:

- A failed attempt (insufficient funds) rolls back the idempotency row, so a retry is re-evaluated. Alternative: record failures too and replay them. Trade-off: replaying a stale failure vs allowing a retry to succeed after the user tops up.
- It does not compare a request fingerprint, so the same key with a different amount is silently treated as a duplicate. Store a hash and reject mismatches.
- It does not check that both accounts match `p_currency`, or `allow_negative`.
- Pessimistic locks vs optimistic version checks (use the `version` column with `WHERE version = $expected` and retry).
- Write the outbox row in the same transaction (see 5.5).

### 5.4 Wallet schema and queries

```sql
CREATE TABLE wallet (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           uuid NOT NULL,
    ledger_account_id uuid NOT NULL UNIQUE REFERENCES account(id),
    currency          char(3) NOT NULL,
    status            text NOT NULL DEFAULT 'active' CHECK (status IN ('active','frozen','closed')),
    daily_limit       bigint NOT NULL DEFAULT 100000 CHECK (daily_limit >= 0),
    created_at        timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, currency)
);

CREATE TABLE wallet_hold (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       uuid NOT NULL REFERENCES wallet(id),
    idempotency_key text NOT NULL UNIQUE,
    amount          bigint NOT NULL CHECK (amount > 0),
    status          text NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active','captured','released','expired')),
    expires_at      timestamptz NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX wallet_hold_active_idx ON wallet_hold (wallet_id) WHERE status = 'active';
```

Available balance = ledger balance minus live holds:

```sql
SELECT b.balance
       - COALESCE(SUM(h.amount) FILTER (WHERE h.status = 'active' AND h.expires_at > now()), 0)
       AS available
FROM wallet w
JOIN account_balance b ON b.account_id = w.ledger_account_id
LEFT JOIN wallet_hold h ON h.wallet_id = w.id
WHERE w.id = $1
GROUP BY b.balance;
```

Place a hold without a race: lock the balance row first, then check and insert in the same transaction.

```sql
BEGIN;
SELECT balance FROM account_balance WHERE account_id = $acct FOR UPDATE;

INSERT INTO wallet_hold (wallet_id, idempotency_key, amount, expires_at)
SELECT $wallet, $key, $amount, now() + interval '7 days'
WHERE (SELECT balance FROM account_balance WHERE account_id = $acct)
      - COALESCE((SELECT SUM(amount) FROM wallet_hold
                  WHERE wallet_id = $wallet AND status = 'active' AND expires_at > now()), 0)
      >= $amount
ON CONFLICT (idempotency_key) DO NOTHING
RETURNING id;      -- no row: insufficient funds or duplicate; check which
COMMIT;
```

Capture a hold (turn it into a real ledger transaction) in one transaction:

```sql
UPDATE wallet_hold SET status = 'captured'
WHERE id = $hold AND status = 'active' AND expires_at > now()
RETURNING amount;          -- zero rows means already captured, released or expired
-- then call post_transfer(...) in the same transaction
```

Statement with running balance (window function):

```sql
SELECT e.id, e.created_at, e.amount,
       SUM(e.amount) OVER (ORDER BY e.id) AS running_balance
FROM ledger_entry e
WHERE e.account_id = $1
ORDER BY e.id DESC
LIMIT 50;   -- for paging, use WHERE e.id < $last_seen (keyset), not OFFSET
```

Discussion points: a wallet read model fed from Kafka vs reading the ledger directly; exact vs approximate daily limits; hold expiry via a scheduled sweep vs filtering on `expires_at`.

### 5.5 Gateway schema: idempotency, payments, webhooks, outbox, inbox

```sql
CREATE TABLE idempotency_key (
    tenant_id       uuid NOT NULL REFERENCES tenant(id),
    key             text NOT NULL,
    request_hash    bytea NOT NULL,
    status          text NOT NULL CHECK (status IN ('in_progress','completed')),
    response_status int,
    response_body   jsonb,
    locked_at       timestamptz NOT NULL DEFAULT now(),
    expires_at      timestamptz NOT NULL DEFAULT now() + interval '24 hours',
    PRIMARY KEY (tenant_id, key)
);

-- Claim the key. One row returned means you own the request.
INSERT INTO idempotency_key (tenant_id, key, request_hash, status)
VALUES ($1, $2, $3, 'in_progress')
ON CONFLICT (tenant_id, key) DO NOTHING
RETURNING 1;
-- No row returned: SELECT the existing row, then
--   completed + same hash  -> replay stored response
--   completed + diff hash  -> 422 key reuse with a different payload
--   in_progress            -> 409 (or wait), unless locked_at is stale (crash recovery)
```

```sql
CREATE TABLE payment (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    uuid NOT NULL REFERENCES tenant(id),
    amount       bigint NOT NULL CHECK (amount > 0),
    currency     char(3) NOT NULL,
    status       text NOT NULL DEFAULT 'created' CHECK (status IN
                 ('created','authorised','captured','settled','failed','voided','refunded','unknown')),
    provider     text,
    provider_ref text,
    version      int NOT NULL DEFAULT 0,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_ref)
);

-- A state transition is a guarded update, never read-modify-write in application code.
UPDATE payment SET status = 'captured', version = version + 1, updated_at = now()
WHERE id = $1 AND status = 'authorised'
RETURNING id;     -- zero rows: illegal or duplicate transition
```

Provider webhooks and consumer-side dedupe:

```sql
CREATE TABLE webhook_event (
    provider     text NOT NULL,
    event_id     text NOT NULL,
    payload      jsonb NOT NULL,
    received_at  timestamptz NOT NULL DEFAULT now(),
    processed_at timestamptz,
    PRIMARY KEY (provider, event_id)
);

INSERT INTO webhook_event (provider, event_id, payload) VALUES ($1, $2, $3)
ON CONFLICT DO NOTHING RETURNING 1;   -- no row means duplicate: acknowledge and stop
```

Outbox and a safe relay worker:

```sql
CREATE TABLE outbox (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aggregate_id uuid NOT NULL,
    event_type   text NOT NULL,
    payload      jsonb NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz
);
CREATE INDEX outbox_unpublished_idx ON outbox (id) WHERE published_at IS NULL;

-- Relay worker, in one transaction: claim, publish to Kafka, mark, commit.
BEGIN;
SELECT id, aggregate_id, event_type, payload FROM outbox
WHERE published_at IS NULL ORDER BY id LIMIT 100
FOR UPDATE SKIP LOCKED;
-- publish each to Kafka and wait for acks, then:
UPDATE outbox SET published_at = now() WHERE id = ANY($ids);
COMMIT;       -- a crash before commit means a re-publish: consumers must be idempotent
```

Known catch to discuss in an interview: identity values are assigned at insert but become visible at commit, so a slow transaction with a lower id can commit after a faster one with a higher id, and a poller that remembers a high-water mark can skip it. Fixes: poll with `published_at IS NULL` (as above) rather than a cursor, or use logical decoding (Debezium), which reads in commit order.

### 5.6 Experiments to run in two psql sessions

1. **Lost update.** Session A and B both `SELECT balance`, add 10 in the application, `UPDATE`, with `pg_sleep(5)` between. See one write vanish. Fix three ways: atomic `UPDATE ... SET balance = balance + 10`, `SELECT ... FOR UPDATE`, or `SERIALIZABLE` with retry.
2. **Write skew.** A joint limit across two accounts: each session checks the sum then withdraws from a different account. Only SERIALIZABLE (or a lock on a shared row) prevents it. Compare Repeatable Read.
3. **Deadlock.** Two transfers in opposite directions without ordered locking; observe SQLSTATE 40P01. Then add `ORDER BY account_id FOR UPDATE`.
4. **Duplicate request.** Run `post_transfer` twice concurrently with the same key; confirm one transaction and two identical return values.
5. **SKIP LOCKED.** Start two relay workers; confirm they never claim the same outbox row.
6. **Retry loop.** Under SERIALIZABLE, catch 40001 and 40P01 in application code and retry the whole transaction with backoff.
7. **Invariant check.** After a load test with 100 concurrent transfers, assert `SUM(amount) = 0` over the ledger and that the balance-cache query above returns no rows.

## 6. Interviewing in Java

Monzo says pair coding is offered in Go, Python and Java, take-home can be in any language, and job posts list Java among the acceptable strongly typed languages. Java is a legitimate choice. The risk is not the language; it is rusty fundamentals and over-reliance on Spring, which will not be there in a 45-minute pair session.

### 6.1 What the coding stage looks like and how to train for it

Format (from Monzo): about 45 minutes, a given interface, unit tests already written, you share your screen and drive in your own IDE, docs and Google allowed, finishing everything is not required, and the interviewer asks about your choices and data-structure trade-offs.

Training method:

1. Set up a plain Maven or Gradle project with JUnit 5, no Spring. Be able to do this in two minutes.
2. Practise on Exercism's Java track (free, test-driven, same shape as the exercise): https://exercism.org/tracks/java
3. Do the practice tasks below against self-written interfaces and tests, timed to 45 minutes, narrating aloud.
4. After each one, write down the data structure you chose, its complexity, and one alternative.

### 6.2 Practice tasks in the style Monzo describes (practical, day-to-day work, my inference of topics)

1. In-memory account store: `deposit`, `withdraw`, `transfer`, `balance`, with insufficient-funds errors and money as long minor units.
2. Idempotent payment processor: `process(request)` where repeated request ids return the original result.
3. Transaction statement: given a list of transactions, produce per-day totals, running balance, and top N merchants.
4. Rate limiter (fixed window, then token bucket), with an injectable `Clock` so it is testable.
5. LRU cache with TTL, then make it thread-safe and explain the cost.
6. Retry helper with exponential backoff and jitter, with a max attempts and a predicate for retryable errors.
7. Event deduplicator and ordered replay: events arrive duplicated and out of order per account.
8. State machine for a payment with legal transitions and an audit trail.
9. Parse and validate a CSV or JSON batch of payments, collecting all errors rather than failing on the first.
10. Scheduler: run jobs by priority and due time (PriorityQueue), then discuss concurrency.

For each: write tests first for the happy path, then edge cases (empty input, duplicates, negative amounts, overflow, concurrent calls).

### 6.3 Java 21 language and library fluency checklist

- Collections and complexity: `ArrayList`, `LinkedList` (rarely right), `HashMap`, `LinkedHashMap` (access order for LRU), `TreeMap`, `PriorityQueue`, `ArrayDeque`, `EnumMap`; the equals and hashCode contract; mutable keys; `ConcurrentModificationException`; defensive copies and `List.copyOf`.
- Modern Java: records, sealed interfaces, pattern matching for `switch`, text blocks, `var`, `Optional` (return type only), streams (readable, not clever), `SequencedCollection` (Java 21).
- Money and time: `long` minor units or `BigDecimal` with explicit `RoundingMode`; never `double`; `Instant`, `Clock` injection, `Duration`.
- Errors: checked vs unchecked, domain exceptions, try-with-resources, not swallowing exceptions, not using exceptions for normal control flow.
- Design: interfaces and composition, immutability, small classes, naming, SOLID applied lightly, constructor injection without a framework.
- Generics, `Comparator` chains, iterators, `hashCode` pitfalls with floating point.

### 6.4 Concurrency and JVM topics to be able to explain without notes

- Thread vs process (a reported Monzo question), context switch cost, virtual threads (JEP 444, final in Java 21).
- `synchronized`, `volatile`, `ReentrantLock`, `Atomic*`, `ConcurrentHashMap` (`compute`, `merge`), `ExecutorService`, `CompletableFuture`, deadlock, livelock, starvation, thread-safe publication.
- The Java Memory Model and happens-before.
- JVM basics: stack vs heap, garbage collection generations and pauses, memory leaks (static maps, unbounded caches), `jstack` and heap dumps.
- Resources: JLS chapter 17 https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html ; JEP 444 https://openjdk.org/jeps/444 ; Jenkov's concurrency tutorial http://tutorials.jenkov.com/java-concurrency/index.html ; dev.java https://dev.java/learn/ ; JUnit 5 guide https://junit.org/junit5/docs/current/user-guide/ ; jqwik property testing https://jqwik.net/ . Books worth buying: Effective Java (Bloch) and Java Concurrency in Practice (Goetz).

### 6.5 Fundamentals reported in interviews

What an HTTP request is end to end (DNS, TCP, TLS, request line, headers, body, status codes, idempotent methods, keep-alive), recursion and its stack cost, thread vs process, hashing and hash collisions, Big-O of the structures you used, how a database index works, what a transaction guarantees. Use the C work in section 8 to make these answers concrete.

### 6.6 Spring knowledge for the project deep dive (not for the pair exercise)

Be ready to explain, from your own gateway: where transaction boundaries are and why (`@Transactional` works through proxies, so self-invocation bypasses it), connection pool sizing (HikariCP), N+1 queries and fetch strategies, optimistic locking with `@Version`, why you used Flyway and `ddl-auto=validate`, how idempotency and outbox are implemented, how you test with Testcontainers, and what you would change at ten times the load.

### 6.7 Take-home in Java: checklist

- One command to build and test (`./mvnw verify`); no required infrastructure unless the task says so; minimal dependencies.
- Meets the task sheet exactly; no extras. Clear package structure; small methods; immutable value types.
- Tests that read like a specification, including edge cases and a concurrency test if shared state exists.
- README: how to run, structure, design decisions and trade-offs, known limitations, what you would do next. Reviewers read it first and run your code.
- Be ready to explain every data structure, complexity, and why you did not use the alternatives.

## 7. Rewrite part of the system in Go (8 weeks)

Goal: learn Go by rebuilding the interesting slice, not the whole gateway: idempotency, ledger transfer, outbox relay, inbox consumer. Keep the same Postgres schema from section 5 so you can compare behaviour with your Java version. Do not port Spring patterns; write Go the way Go projects are written.

### 7.1 Java to Go mental map

| Java | Go |
| --- | --- |
| Exceptions | Errors are return values; wrap with `fmt.Errorf("...: %w", err)`, test with `errors.Is` and `errors.As` |
| Classes and inheritance | Structs plus composition; no inheritance |
| Explicit `implements` | Interfaces are satisfied implicitly; define small interfaces where they are used |
| Threads and executors | Goroutines, channels, `sync`, `errgroup`; cancellation through `context.Context` |
| Spring DI and annotations | Explicit constructor wiring in `main`; no framework needed |
| Lombok and builders | Plain structs, functional options when needed |
| JUnit and Mockito | `testing` package, table-driven tests, hand-written fakes |
| `null` everywhere | Zero values; `nil` for pointers, slices, maps, interfaces (beware typed-nil interfaces) |
| `try/finally` | `defer` |

Gotchas to study early: loop variable capture (changed in Go 1.22), slice aliasing and `append`, map not safe for concurrent writes, goroutine leaks, forgetting `ctx` on DB and HTTP calls, `defer` in loops, shadowed `err`.

### 7.2 Eight-week plan

- **Week 1: language basics.** Tour of Go, then Learn Go with Tests up to maps, errors and interfaces. Build a `Money` type (int64 minor units, currency) with table-driven tests.
- **Week 2: HTTP service.** `net/http` with the standard router or chi; JSON handlers; validation; middleware for request id, structured logging (`log/slog`) and JWT auth; graceful shutdown with `context`.
- **Week 3: Postgres and the ledger.** `pgx`, `golang-migrate` or Flyway migrations, sqlc for typed queries. Implement `Transfer` using the schema in 5.2 and 5.3, a transaction helper that retries SQLSTATE 40001 and 40P01 with backoff.
- **Week 4: idempotency and concurrency tests.** Idempotency middleware using the table in 5.5. With testcontainers-go and real Postgres, run 100 goroutines sending the same key and 100 random transfers; assert exactly one transaction per key and the ledger sums to zero.
- **Week 5: outbox and Kafka.** Relay worker with `FOR UPDATE SKIP LOCKED`, producer and consumer (franz-go or kafka-go), inbox-based dedupe, `errgroup` for worker lifecycles, graceful stop with context cancellation. Kill the process between commit and publish and show no event is lost.
- **Week 6: resilience and observability.** Timeouts on every outbound call, retry with jitter, a circuit breaker, `golang.org/x/time/rate` plus a Redis token bucket, Prometheus metrics, OpenTelemetry traces.
- **Week 7: correctness tooling.** `go test -race`, benchmarks, `pprof`, fuzz tests (`go test -fuzz`), property tests (rapid). Compare throughput and latency with the Java version and write down why they differ.
- **Week 8: ship and explain.** Multi-stage Dockerfile, Kubernetes manifests with liveness and readiness probes on kind, a README with design decisions and trade-offs. Present it aloud as if in the initial call.

A retry helper to write yourself (do not copy-paste), in this shape:

```go
func WithTx(ctx context.Context, pool *pgxpool.Pool, fn func(pgx.Tx) error) error {
    var err error
    for attempt := 0; attempt < 5; attempt++ {
        err = pgx.BeginTxFunc(ctx, pool, pgx.TxOptions{IsoLevel: pgx.Serializable}, fn)
        var pgErr *pgconn.PgError
        if errors.As(err, &pgErr) && (pgErr.Code == "40001" || pgErr.Code == "40P01") {
            if sleepErr := sleepWithJitter(ctx, attempt); sleepErr != nil {
                return sleepErr
            }
            continue
        }
        return err
    }
    return fmt.Errorf("transaction retries exhausted: %w", err)
}
```

Check the exact pgx v5 API names against its docs before relying on this.

### 7.3 Free Go resources

- Tour of Go: https://go.dev/tour/
- Learn Go with Tests (best way to learn by testing): https://quii.gitbook.io/learn-go-with-tests
- Effective Go: https://go.dev/doc/effective\_go
- Go by Example: https://gobyexample.com/
- 100 Go Mistakes (summary site): https://100go.co/
- Go blog: context https://go.dev/blog/context ; pipelines and cancellation https://go.dev/blog/pipelines ; the memory model https://go.dev/ref/mem
- Go Proverbs: https://go-proverbs.github.io/ ; Rob Pike on concurrency vs parallelism: https://go.dev/blog/waza-talk
- Style: Google Go style guide https://google.github.io/styleguide/go/ ; Uber Go style guide https://github.com/uber-go/guide
- Libraries: pgx https://github.com/jackc/pgx ; sqlc https://sqlc.dev/ ; chi https://github.com/go-chi/chi ; franz-go https://github.com/twmb/franz-go ; go-redis https://github.com/redis/go-redis ; testcontainers-go https://golang.testcontainers.org/ ; OpenTelemetry Go https://opentelemetry.io/docs/languages/go/
- Monzo also publishes open source Go: https://github.com/monzo (read real Go written by the company).

## 8. Learning how computers work, using C

Why C for you: it removes the runtime that hides memory, processes, files and sockets from a web developer. Goal is not to write production C; it is to answer questions like what actually happens on fsync, why Kafka is fast, why Cassandra writes are cheap, what a context switch costs, and what is inside an HTTP request. These are exactly the first-principles questions Monzo says it probes.

### 8.1 Concepts to cover

Stack vs heap, pointers and arrays, process address space and virtual memory, how a program is compiled, linked and loaded, system calls, file descriptors, buffered vs unbuffered IO, the page cache, `fsync` and durability, sockets and TCP, blocking vs non-blocking IO, `select`/`epoll`, processes vs threads, `fork`/`exec`/pipes, mutexes and atomics, CPU caches and why memory layout affects speed, endianness and binary formats.

### 8.2 Free resources, in order

1. **Beej's Guide to C** (start here): https://beej.us/guide/bgc/ . The classic book is K&R, The C Programming Language (paid).
2. **CS:APP and CMU 15-213 (Computer Systems: A Programmer's Perspective).** Course page https://www.cs.cmu.edu/\~213/ ; the self-study labs (data lab, bomb lab, attack lab, malloc lab, shell lab, proxy lab) are at https://csapp.cs.cmu.edu/3e/labs.html . The best single route to understanding computers from a programmer's view; the textbook is paid but the labs and lecture materials are free.
3. **Operating Systems: Three Easy Pieces** (free book): https://pages.cs.wisc.edu/\~remzi/OSTEP/
4. **Beej's Guide to Network Programming**: https://beej.us/guide/bgnet/
5. **Linux man pages** (read them for every syscall you use): https://man7.org/linux/man-pages/ ; The Linux Programming Interface by Kerrisk (paid) is the reference.
6. **Build Your Own Redis** and **Build Your Own Database**: https://build-your-own.org/redis/ and https://build-your-own.org/database/
7. **What Every Programmer Should Know About Memory** (Drepper, free PDF; search the title) and the 'latency numbers every programmer should know' list.
8. Later: MIT 6.S081 operating systems with xv6 (now numbered 6.1810): https://pdos.csail.mit.edu/6.828/ ; Julia Evans' blog and zines on strace, networking and Linux: https://jvns.ca/

Tools: gcc or clang with `-Wall -Wextra -g -fsanitize=address,undefined`, Valgrind https://valgrind.org/docs/manual/ , gdb, `strace` (watch your program's syscalls), `perf`, `make`. Use Linux or WSL2 or a container; macOS works for most of it but `epoll` and `strace` are Linux-only.

### 8.3 What to build, in order, and what it teaches

| # | Build | What you learn | Interview link |
| --- | --- | --- | --- |
| 1 | `cat`, `wc`, `grep` using `open`, `read`, `write` | File descriptors, syscalls, buffering. Run with `strace`. | What happens when a program reads a file |
| 2 | Dynamic array and hash map with tests | Memory management, hashing, resizing, collisions | How HashMap works; Big-O in practice |
| 3 | A tiny shell (`fork`, `exec`, pipes, redirects) | Processes, signals, pipes | Thread vs process |
| 4 | A `malloc` implementation (CS:APP malloc lab) | Heap layout, fragmentation | Memory leaks, GC vs manual |
| 5 | TCP echo server: thread per connection, then an `epoll` event loop, then an HTTP/1.1 parser | Sockets, blocking vs non-blocking, C10K | What is an HTTP request; thread-per-request vs event loop vs goroutines vs virtual threads |
| 6 | **Append-only key-value store with a write-ahead log and `fsync`, and crash recovery** | Durability, torn writes, recovery, checksums | How a database or ledger survives a crash; why Postgres has a WAL |
| 7 | Mini Redis (RESP protocol) | Protocol design, in-memory structures, persistence | Redis internals |
| 8 | LSM tree (memtable, SSTables, compaction) then a disk B-tree | Storage engines | Cassandra writes vs Postgres writes |
| 9 | Capstone in Go or Rust: Raft (MIT 6.5840 labs) | Consensus | Leader election, replication logs |

Project 6 is the most valuable for a ledger engineer. The core of its append path looks like this:

```c
int fd = open(path, O_WRONLY | O_CREAT | O_APPEND, 0644);
ssize_t n = write(fd, buf, len);
if (n != (ssize_t)len) { /* partial write: treat the record as not written */ }
if (fsync(fd) != 0)     { /* data is NOT durable: report failure, do not acknowledge */ }
```

Then test it: kill -9 the process mid-write, truncate the file at random points, flip a byte, and make recovery detect a bad checksum and stop at the last good record. Measure the cost of fsync per write vs batched (group commit). This is the same trade-off Postgres, Kafka and Cassandra make: latency vs durability vs throughput.

Questions you should be able to answer after this work: What does `fsync` guarantee and what happens if you skip it? Why is sequential disk IO fast? What is a page cache? Why is a context switch expensive? Why do event loops scale to many connections? What is in a TCP connection and what does it cost? Why can a hash map degrade? Why is a ledger append-only?

## 9. Combined study plan (about 12 hours a week, 16 weeks)

Weekly rhythm: 2 sessions systems reading and design (3h), 2 build sessions (4h), 1 timed Java practice (2h), 1 C session (2h), 1 mock design or behavioural practice aloud (1h).

| Weeks | Focus | Output |
| --- | --- | --- |
| 1 to 2 | Read Monzo's interview post fully. DDIA chapters 5 to 7 (replication, partitioning, transactions). Run the section 5.6 Postgres experiments. Java: tasks 1 to 3 timed. C: Beej's guide, build 1 and 2. | A notes file: each anomaly you reproduced and how you fixed it |
| 3 to 4 | DDIA chapters 8 to 9 (distributed trouble, consistency and consensus). Build the ledger schema and `post_transfer`, with concurrency tests. Java: tasks 4 to 6. C: build 3 (shell). | Ledger repo with passing concurrent invariant test |
| 5 to 8 | Go plan weeks 1 to 4 (section 7). Wallet holds and limits in Postgres. DDIA chapter 11 (streams) and Kafka design doc. Java: tasks 7 to 10. C: build 4 and 5. | Go service with idempotent transfer; wallet design doc |
| 9 to 12 | Go plan weeks 5 to 8 (outbox, Kafka, resilience, race detector, Kubernetes on kind). Redis rate limiter. C: build 6 (WAL store). Start MIT 6.5840 lecture videos. Begin two design mocks per week. | Gateway slice in Go with outbox, inbox, retries, metrics |
| 13 to 14 | System design practice: 8 problems from the list in section 2 plus the System Design Primer. Write the one-page design doc for your best project and practise the one-hour deep dive. | Two recorded 45-minute design walkthroughs |
| 15 to 16 | Behavioural stories (6, in STAR form), mock take-home review, review Monzo's blog posts and engineering talks, read Cassandra basics. Apply. | Polished GitHub repos with READMEs |

You do not need to finish all 16 weeks before applying. Monzo reports a process of about 2 to 3 weeks, so starting applications around week 8 to 10 is reasonable, using the remaining time as interview preparation.

## 10. How to run a system design answer (Monzo-style)

1. **Clarify** functional requirements, scale, latency, consistency and durability needs. State assumptions out loud.
2. **API and data model** first: the main entities, keys, and the invariants that must never break.
3. **Draw the happy path** (clients, services, databases, queues) and name the information flow.
4. **Choose storage and messaging from first principles.** Say why this database (access pattern, consistency need) and what you give up. Avoid naming a product you cannot reason about; if you name AWS or GCP services, explain what they do underneath.
5. **Walk the failure modes:** duplicate request, crash mid-operation, slow dependency, hot key, replica lag, message redelivery, out-of-order events.
6. **Scale it:** partitioning key, bottlenecks, caching, back-pressure.
7. **Trade-offs summary:** consistency vs availability vs latency choices you made, and what you would revisit.
8. **Keep a shared understanding with the interviewer.** Summarise often; adapt when they add information. Neatness of diagrams does not matter; shared understanding does.

### One-page design doc template for your own projects (use for the initial call)

Problem and users; requirements and non-goals; architecture diagram; data model and invariants; key decisions with alternatives considered and why rejected; failure modes and how each is handled; testing strategy; metrics and operations; what you would do differently; your personal contribution.

### Behavioural story bank (Monzo covers communication, learning, teamwork, delivery)

Prepare one real story each for: a complex project you delivered incrementally; a disagreement you resolved with a colleague or stakeholder; a mistake or incident and what changed afterwards; something you learned fast; a time you improved someone else's work or the team's process; a time you worked across functions. For each, state the situation, what you personally did, the result, and what you would change.

## 11. Closing notes

- URLs outside Monzo's own page were written from memory. If one fails, search the title; the project home pages are stable.
- Interview reports change over time and Monzo updates its process; re-read https://monzo.com/blog/demystifying-the-backend-engineering-interview-process and the current job post before each stage.
- Monzo does not allow LLMs during interviews, so use AI to prepare, then practise without it: whiteboard, code and explain on your own.
- Depth beats breadth. A single project you can defend for an hour, with real failure testing, is worth more than a long list of technologies.

  |  |  |  |
  | --- | --- | --- |
  |  |  |  |
  |  |  |  |

## 12. Interview questions reported on the Internet (Monzo backend engineer, including Backend Engineer III)

Compiled 20 November 2026 from public candidate reports. Read these limits first:

- **This is everything I could find, not everything that exists.** Interviewers choose questions, reports are anonymous and unverified, and Glassdoor blocks automated page reading, so several items come from search-result snippets and may be incomplete.
- **Almost no reports are tagged specifically Backend Engineer III.** Monzo runs one standard process across levels, so reports for Backend Engineer, Software Engineer and L30 to L60 roles are included and labelled. One Backend Engineer III report (August 2025) described the process as friendly and straightforward, going through the CV and past work.
- **Questions are paraphrased.** The answer outlines below are my suggestions, not Monzo's answers.
- **Correction to section 2:** I stated that Backend Engineer III maps to Monzo's L40 level. I could not re-verify that. One candidate report says they applied at L30 (mid-level) for a backend role, and job posts for Engineer III list pay roughly in the 78,000 to 110,000 GBP range (figures vary by post and location). Treat levelling as unconfirmed and ask your recruiter.
- **Pass rate context:** one third-party aggregator (Taro) reports a pass rate of about 29 percent across 45 Monzo backend interview reports. It is a self-selected sample, so treat it as indicative only.

### 12.1 Project and experience questions (initial call and behavioural)

| Reported question or prompt | What a strong answer covers |
| --- | --- |
| Talk through a project you led or your current project in great technical detail, including how you handled stakeholders | Problem and constraints, your role vs the team's, design alternatives you rejected, data and failure handling, metrics, what you would change, who you aligned and how |
| Describe your experience designing distributed systems | One real system: partitioning, consistency choices, failure you hit, how you detected and fixed it |
| Describe the architecture of a system you know well as if explaining to a junior engineer or new teammate | Start with the purpose and user flow, then components, data stores, one request end to end, then known weak spots. Clear, no jargon dumping |
| Which of your projects challenged you most? What type of learner are you? | A concrete hard problem and how you broke it down; how you learn (reading, building, pairing) with an example |
| What drives you as an engineer? What do you get most out of your work? | Honest, specific motivation tied to customer or system outcomes |
| How would you find out whether someone is a good engineer? | Signals beyond output: quality of reasoning, collaboration, learning rate, ownership, how they handle failure; how you would gather evidence (code review, design discussions, incidents) |
| Tell me about difficult feedback you received (reported for an adjacent web role, plausible here) | Situation, what you heard, what you changed, evidence it improved |

One candidate noted that the behavioural interview rewards confident first-person ownership ('I did this'), which matches Monzo's own advice to separate your contribution from the team's. Another reported being rejected after the system design stage for not showing enough leadership confidence, so prepare to take clear positions and defend them.

### 12.2 Technical fundamentals reported

| Reported question | What a strong answer covers |
| --- | --- |
| What is an HTTP request? | Client sends method, path, headers, optional body over TCP (usually TLS); server returns status, headers, body. Cover DNS, connection setup, idempotent methods, status code classes, keep-alive, statelessness, cookies and auth headers |
| How does the Internet work? (what happens when you open a URL) | DNS resolution, TCP handshake, TLS handshake, HTTP request, load balancer and CDN, application, database, response, rendering. Name caching at each layer and where it can fail |
| What is the purpose of the TCP three-way handshake? | SYN, SYN-ACK, ACK: both sides agree initial sequence numbers and confirm the other can send and receive; protects against stale duplicate connection attempts; costs one round trip; SYN floods and mitigations; how TLS adds more round trips |
| What is the difference between a thread and a process? | Separate vs shared address space, isolation and crash containment, creation and context-switch cost, communication (IPC vs shared memory), scheduling; relate to JVM threads and virtual threads, goroutines |
| How would a mutex be implemented internally? | Atomic compare-and-swap or test-and-set on a flag in user space; contention path parks the thread via the OS (futex on Linux) instead of spinning forever; memory barriers; fairness and priority inversion; why spinlocks suit only very short critical sections |
| Describe recursion | Function calling itself with a base case; stack frames and overflow risk; converting to an iterative form with an explicit stack or queue; tail calls are not optimised in Java or guaranteed in Go. One reviewer was challenged on recursion in a take-home review, so be ready to justify it |
| General question about 'how some technical stuff works' | Be able to explain databases, queues, caches, DNS, TLS, indexes and garbage collection from first principles (Monzo's own guide says it prefers first-principles reasoning) |

Use section 8 (C projects) to make these answers concrete.

### 12.3 Architecture and trade-off questions reported

| Reported question | What a strong answer covers |
| --- | --- |
| Would you keep an application as a single deployed monolith or turn it into a distributed, horizontally scalable application? | Avoid a dogma answer. Decide using team size, deployment independence, scaling hot spots, consistency boundaries, operational cost and failure isolation. Mention a modular monolith as a middle path. Horizontal scaling needs stateless services, partitioned data and idempotent operations. Show you understand what Monzo's own choice (many small services) costs: tracing, data consistency across services, deployment tooling |
| What does a cache do, and what happens when it goes down? Do you have a backup? (reported in a Backend Engineer III interview, August 2025) | A cache is an optimisation, not the source of truth. On failure: fall back to the database but protect it (rate limits, request coalescing, circuit breaker), avoid thundering herd on restart (jittered TTLs, warm-up), run a replicated or clustered cache with failover, size the database for a cold cache, decide fail-open vs fail-closed per use case (never fail open on authorisation or idempotency checks). Mention what data lives only in the cache and whether that is acceptable |
| Take-home follow-ups on a crawler: scaling, retry mechanisms, advantages and disadvantages of each data structure | See 12.4 |

### 12.4 Coding exercises reported

**Web crawler take-home (reported in older Glassdoor reviews around 2017 to 2019, in Go and other languages).** Reviewers asked about scaling, retries, and data-structure trade-offs.

- Use an explicit queue (breadth-first) instead of deep recursion; track visited URLs in a hash set after URL normalisation (scheme, host case, fragments, trailing slashes).
- Concurrency: a bounded worker pool with cancellation (context in Go, executor shutdown in Java), a mutex or concurrent set for visited, no unbounded goroutines or threads.
- Politeness and safety: stay within a domain, honour robots.txt, per-host rate limits, timeouts on every request, a maximum depth or page count.
- Retries: exponential backoff with jitter, only for retryable errors (timeouts, 5xx, 429), a maximum attempts cap.
- Testability: an interface for the fetcher so tests use a fake site; a README explaining structure and trade-offs, as Monzo asks.
- Scaling discussion: partition the frontier queue by host, shared visited store (Redis set or a Bloom filter with its false-positive trade-off), idempotent processing of pages, backpressure.

**In-memory bank ledger with transactions (reported as the pair coding task in an October 2025 report for a mid-level role, and as live coding of an in-memory ledger in another report; one candidate said the interviewer mostly watched and gave occasional input).** This is the closest thing to a reported Backend III style coding task, so practise it until it is routine.

- Interface sketch: create account, deposit, withdraw, transfer, get balance, get transaction history, possibly reverse a transaction.
- Model money as integer minor units, not floats. Keep transactions immutable and append-only; derive or cache balances.
- Invariants: no negative balance unless allowed, transfers are atomic (both sides or neither), the sum across accounts is conserved.
- Edge cases and errors: unknown account, same-account transfer, zero or negative amount, insufficient funds, duplicate transaction id (idempotency), integer overflow.
- Thread safety: one lock per account acquired in a fixed order to avoid deadlock, or a single lock first for simplicity; say what you would change at scale.
- Tests you write along the way, including a concurrent-transfers test that asserts the total is unchanged.
- Talk about trade-offs aloud: data structure for accounts (HashMap) and history (ArrayList per account), complexity, memory, and what changes when persisted.

**Take-home review call (reported across years).** Expect to justify decisions, discuss edge cases and error handling, explain whether your tests are adequate, and say what you would improve. A Monzo engineer's own account of his interview says the review focused on how the design and structure came about and whether the tests were sufficient.

### 12.5 System design reports

No reviewer I found published the actual prompt. What is reported:

- A hypothetical, non-Monzo problem, sketched with an engineer in a shared drawing tool, with new information added as you go (Monzo's own guide).
- One report says you are given a partially designed system and asked to complete it. Practise this by taking a basic diagram (client, API, database) and finding what is missing: retries, idempotency, queues, dead letters, monitoring, scaling, failure of each box.
- Candidates call it the hardest stage; several unsuccessful reports cite lack of system design experience, and one cites not showing enough confidence. A Monzo engineer who joined said he prepared with a system design course and by building GitHub projects.
- Use the practice prompts in section 2 and the framework in section 10.

### 12.6 Coding-style notes from reports

- No LeetCode-style puzzles are reported; hires say they did not meet them at Monzo.
- Monzo's US hiring post lists Go, Java, Python and C# as languages for the pair exercise, so Java is explicitly fine.
- Reports vary on interviewer quality: some describe a streamlined, friendly process, some describe thin interviewer engagement. Prepare to drive the session yourself and think aloud.

### 12.7 Sources

- Glassdoor, Monzo Backend Engineer interview questions: https://www.glassdoor.co.uk/Interview/Monzo-Bank-Backend-Engineer-Interview-Questions-EI\_IE1557148.0,10\_KO11,27.htm (more pages exist; Glassdoor may require a login)
- Glassdoor, Monzo interview questions overview (includes the August 2025 Backend Engineer III report): https://www.glassdoor.com/Interview/Monzo-Bank-Interview-Questions-E1557148\_P2.htm
- Individual Glassdoor reviews: HTTP and recursion (2019) https://www.glassdoor.com.au/Interview/Monzo-Bank-Interview-E1557148-RVW30970426.htm ; thread vs process (2020) https://www.glassdoor.co.uk/Interview/Monzo-Bank-Interview-E1557148-RVW37039199.htm ; how the Internet works https://www.glassdoor.co.nz/Interview/Monzo-Bank-Interview-E1557148-RVW21013660.htm ; live coding an in-memory ledger https://www.glassdoor.com.hk/Interview/Monzo-Bank-Interview-E1557148-RVW74797931.htm
- Taro, Backend Engineer report (October 2025, bank ledger pair task): https://www.jointaro.com/interviews/companies/monzo/experiences/backend-engineer-october-23-2025-no-offer-negative-c99cc06c/
- Monzo engineer, 'My first 6 months at Monzo as a Backend Engineer': https://monzo.com/blog/2022/02/17/my-first-6-months-at-monzo-as-a-backend-engineer
- Monzo US, 'Interviewing at Monzo': https://monzo.com/blog/monzo-us-blog/interviewing-at-monzo
- Monzo, 'Demystifying the Backend Engineering interview process': https://monzo.com/blog/demystifying-the-backend-engineering-interview-process

Re-check these pages before your interview; new reports appear regularly.
