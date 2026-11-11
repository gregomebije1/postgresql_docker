# Distributed Financial Systems Architecture: Accounting Ledger Manual

This manual defines the production-grade Chart of Accounts (COA) and Double-Entry Journal Entries for two tightly decoupled systems:

- **The E-Wallet Orchestrator Engine:** A customer-facing, assetless pass-through engine that leases balances, holds no physical cash custody, and delegates funding/payout actions to an external gateway.
- **The Payment Gateway Clearing Engine:** An infrastructure pipeline that manages incoming processor receivables, calculates basis-point transaction fee splits, and drives merchant settlements.

---

## How to Read This Manual (Learning Path)

This is written from the standpoint of an engineer building a bank-grade money platform (the kind of problem a Monzo-scale team faces). It is a **teaching model**, not any bank's internal design. Detail increases level by level, so stop at the level you need and come back later:

1. **Business:** Parts 1-2 in plain English. Who owes whom, and when.
2. **Accounting:** the journal tables in Parts 1-2. Every movement is a balanced debit and credit.
3. **Data:** Part 4. Schemas and the exact PostgreSQL statements for each step.
4. **Concurrency:** Parts 5-6. What breaks when two things happen at once.
5. **Failure:** Part 7. What happens when networks and services fail.
6. **Theory and code:** Parts 8-9. Concept map with DDIA references, and how to turn this into code.

*DDIA* = Martin Kleppmann, *Designing Data-Intensive Applications* (O'Reilly, 2017).

---

## Part 1: The E-Wallet Orchestrator Engine

The E-Wallet engine serves as a customer-facing transactional interface. Because it holds no direct custody of the underlying cash assets, its ledger focuses on managing receivable claims against the external gateway and tracking user balances as platform liabilities.

### 1. Chart of Accounts (COA)

| Account Number | Account Name | Type | Description |
| --- | --- | --- | --- |
| 1210 | Gateway Funding Receivables | Asset | Floating claims against the external gateway for incoming customer wallet deposits. |
| 1220 | Gateway Payout Clearing | Asset | Transient tracking lane for outbound customer bank withdrawals routed through the gateway. |
| 1230 | Settlement Bank Account | Asset | Corporate bank account where the gateway deposits bulk payouts. |
| 2210 | User Wallet Balances (Liability Pool) | Liability | The digital ledger balance leased to the user, reflecting their available spending power. |
| 2220 | Saga Transaction Holds | Liability | Temporary escrow pool holding user funds during active multi-network workflows (e.g., utility purchases). |
| 4210 | Commission & Markup Revenue | Revenue | Margins earned by adding a convenience fee on top of the gateway's base pricing. |
| 5210 | Gateway Processing Expense | Expense | Interchange and processing costs deducted by the external provider. |

### 2. Core Transactional Flows & Journal Entries

#### Flow A: Multi-Channel Wallet Funding (Incoming Deposit)

A user initiates a $100.00 deposit via credit card inside the wallet app. The wallet engine charges the user a $3.00 convenience fee margin on top of the transaction.

**Step 1: Upon Webhook Ingress Affirmation** *(Customer's card is successfully charged by the gateway)*

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 1210 – Gateway Funding Receivables | +$103.00 | Asset increases: The gateway now owes you the deposit + your margin |
| Credit | 2210 – User Wallet Balances | +$100.00 | Liability increases: User's available digital balance is topped up |
| Credit | 4210 – Commission & Markup Revenue | +$3.00 | Revenue increases: Your platform fee margin is recognized |

**Step 2: Gateway Bulk Settlement** *(Occurs 24–48 hours later)*

The external gateway takes its own processing fee cut ($1.50) and deposits the remaining net balance ($101.50) into your corporate settlement bank account.

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 1230 – Settlement Bank Account | +$101.50 | Asset increases: Physical cash hits your actual corporate bank |
| Debit | 5210 – Gateway Processing Expense | +$1.50 | Expense increases: The gateway's fee cut is recorded |
| Credit | 1210 – Gateway Funding Receivables | -$103.00 | Asset decreases: Clears out your pending claim against the gateway |

> **Net platform profit tracked on this transaction block:** $3.00 markup revenue − $1.50 gateway expense = **$1.50 clear margin**.

---

#### Flow B: Peer-to-Peer (P2P) Internal Transfer

User 1 transfers $25.00 to User 2 within the wallet ecosystem. Because this action is entirely internal and does not involve external banking movements, no asset accounts are altered.

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 2210 – User Wallet Balances (User 1) | -$25.00 | Liability decreases: Platform liability to User 1 decreases |
| Credit | 2210 – User Wallet Balances (User 2) | +$25.00 | Liability increases: Platform liability to User 2 increases |

---

#### Flow C: Value-Added Service (VAS) Utility Purchase

A user purchases an airtime data subscription for $10.00.

**Step 1: Saga Orchestrator Initialization** *(Place a hold on funds to prevent double-spending race conditions)*

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 2210 – User Wallet Balances | -$10.00 | Deducted from the user's active spending lane |
| Credit | 2220 – Saga Transaction Holds | +$10.00 | Moved into a temporary escrow liability lock |

**Step 2: Third-Party Utility API returns a success payload**

The hold is released and the external gateway is instructed to pay out the utility vendor from your corporate settlement account.

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 2220 – Saga Transaction Holds | -$10.00 | The internal transaction lock is cleared |
| Credit | 1220 – Gateway Payout Clearing | +$10.00 | Asset decreases: Your clearing account tracks that cash is leaving the system |

---

## Part 2: The Payment Gateway Clearing Engine

The Payment Gateway operates as a clearing broker for merchant tenants. It interfaces directly with card networks and acquiring banks, processing gross receivables, extracting platform processing fees, and managing payout corridors.

### 1. Chart of Accounts (COA)

| Account Number | Account Name | Type | Description |
| --- | --- | --- | --- |
| 1110 | Processor Receivables (In-Transit) | Asset | Funds captured by card networks but not yet deposited into the gateway's bank. |
| 1120 | Settlement Bank Account | Asset | Corporate bank account used to receive network settlements and dispatch payouts. |
| 2110 | Merchant Settlement Payables | Liability | Net funds owed to your merchant tenants that are available for withdrawal. |
| 4110 | Gateway Processing Fee Revenue | Revenue | Gross margins earned by the platform from your variable/fixed transaction fee splits. |
| 5110 | Interchange/Processor Expense | Expense | Upstream interchange fees charged by core Visa/Mastercard card networks or card factories. |

### 2. Core Transactional Flows & Journal Entries

#### Flow A: A Customer Pays a Merchant $100.00

The customer buys a product from a merchant using your gateway proxy. Your automated fee engine calculates a $2.80 platform processing fee split.

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 1110 – Processor Receivables (In-Transit) | +$100.00 | Asset increases: Core network card processor owes you the gross transaction amount |
| Credit | 2110 – Merchant Settlement Payables | +$97.20 | Liability increases: Net balance owed to your merchant tenant |
| Credit | 4110 – Gateway Processing Fee Revenue | +$2.80 | Revenue increases: Your platform's gross processing fee margin |

---

#### Flow B: Card Network Settles Cash into the Gateway Account

The card network settles the transaction, deducting an upstream interchange cost of $1.10 before moving the cash into your account.

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 1120 – Settlement Bank Account | +$98.90 | Asset increases: Cash physically received from the processor |
| Debit | 5110 – Interchange/Processor Expense | +$1.10 | Expense increases: Cost deducted by the upstream processing card bank |
| Credit | 1110 – Processor Receivables (In-Transit) | -$100.00 | Asset decreases: In-transit clearing balance resets to zero |

> **Net platform profit for this specific lifecycle:** $2.80 Gross Revenue − $1.10 Interchange Expense = **$1.70 net margin**.

---

#### Flow C: Asynchronous Payout Dispatched to Merchant Account

The merchant triggers a withdrawal request to transfer their settled earnings ($97.20) out of the gateway into their local corporate business bank account.

| Entry | Account | Amount | Effect |
| --- | --- | --- | --- |
| Debit | 2110 – Merchant Settlement Payables | -$97.20 | Liability decreases: The platform has cleared its debt obligation to the merchant |
| Credit | 1120 – Settlement Bank Account | -$97.20 | Asset decreases: Cash leaves your settlement bank account |

---

## Part 3: Automated Three-Way Reconciliation

Because the E-Wallet engine relies on external systems, it uses an asynchronous background worker loop to perform Three-Way Reconciliation. It matches internal saga execution logs against incoming gateway transaction files and actual bank data feeds.

```
┌───────────────────────────────┐        ┌───────────────────────────────┐
│     Saga Orchestration Logs   │        │   Gateway Settlement Export   │
│   (Internal Intent Register)  │        │   (External API / CSV Feed)   │
└───────────────┬───────────────┘        └───────────────┬───────────────┘
                │                                         │
                └───────────────────┬─────────────────────┘
                                     ▼
                     ┌──────────────────────────────┐
                     │  Automated Reconciler Engine  │
                     └──────────────┬───────────────┘
                                     │
                                     ▼
                     ┌──────────────────────────────┐
                     │    Bank Statement Feed        │
                     │     (MT940 / BAI2 Format)     │
                     └──────────────────────────────┘
```

The background thread executes this reconciliation process by running a high-performance filtering query to identify discrepancy anomalies where internal ledger intents do not match external reality records:

```sql
SELECT 
    sol.id AS saga_id,
    sol.trace_id,
    sol.amount AS internal_expected_amount,
    wfi.amount AS gateway_intended_amount,
    wfi.provider_intent_id AS external_reference,
    (sol.amount - wfi.amount) AS balance_discrepancy_delta
FROM saga_orchestration_logs sol
JOIN wallets w ON sol.wallet_id = w.id
JOIN wallet_funding_intents wfi ON wfi.wallet_id = w.id
WHERE sol.status = 'COMPLETED'
  AND wfi.status = 'SUCCESS'
  -- Flag an exception anomaly if the internal ledger does not match gateway intent data
  AND sol.amount <> wfi.amount;
```

If the query returns data rows, the automated workflow flags those transaction records as `DISCREPANCY_LOCKED` and sends them to a manual audit review stream, ensuring zero data drift passes into your system.

---

## Part 4: Implementation as Three Databases (PostgreSQL)

Each system owns its own database and never writes to another one. There are **no cross-database transactions**: systems talk through events (a transactional *outbox* on the sender, an idempotent *inbox* on the receiver). Amounts are integer cents. Examples use user 1 (wallet 1) and user 2 (wallet 2).

### 4.1 Schemas

**Every database** has the same two messaging tables:

```sql
CREATE TABLE outbox (
  id BIGSERIAL PRIMARY KEY, topic TEXT NOT NULL, payload JSONB NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now(), published_at TIMESTAMPTZ);
CREATE TABLE inbox (event_id TEXT PRIMARY KEY, received_at TIMESTAMPTZ DEFAULT now());
```

**wallet_db** (E-Wallet Engine)

```sql
CREATE TABLE wallets (
  id BIGSERIAL PRIMARY KEY, user_id BIGINT UNIQUE NOT NULL,
  available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0),
  held      BIGINT NOT NULL DEFAULT 0 CHECK (held >= 0));
CREATE TABLE wallet_txns (
  id BIGSERIAL PRIMARY KEY, 
  idem_key TEXT UNIQUE NOT NULL,
  wallet_id BIGINT NOT NULL REFERENCES wallets(id),
  kind TEXT NOT NULL,            -- DEPOSIT | P2P | VAS
  amount BIGINT NOT NULL, fee BIGINT NOT NULL DEFAULT 0,
  counterparty_wallet BIGINT,
  status TEXT NOT NULL,          -- PENDING | SUCCESS | FAILED
  created_at TIMESTAMPTZ DEFAULT now());
```

**ledger_db** (double-entry books for both systems)

```sql
CREATE TABLE accounts (code INT PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL);
CREATE TABLE journal_entries (
  id BIGSERIAL PRIMARY KEY, ref TEXT UNIQUE NOT NULL,   -- idempotency key
  memo TEXT, posted_at TIMESTAMPTZ DEFAULT now());
CREATE TABLE journal_lines (
  id BIGSERIAL PRIMARY KEY,
  entry_id BIGINT NOT NULL REFERENCES journal_entries(id),
  account_code INT NOT NULL REFERENCES accounts(code),
  wallet_id BIGINT,                                     -- sub-ledger tag for 2210
  debit BIGINT NOT NULL DEFAULT 0, credit BIGINT NOT NULL DEFAULT 0,
  CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)));


**gateway_db** (Payment Gateway)

```sql
CREATE TABLE tenant_balances (
  tenant TEXT PRIMARY KEY, available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0));
CREATE TABLE payments (
  id BIGSERIAL PRIMARY KEY, provider_ref TEXT UNIQUE NOT NULL, tenant TEXT NOT NULL,
  amount BIGINT NOT NULL, fee BIGINT NOT NULL,
  status TEXT NOT NULL);         -- CAPTURED | SETTLED
CREATE TABLE payouts (
  id BIGSERIAL PRIMARY KEY, idem_key TEXT UNIQUE NOT NULL, tenant TEXT NOT NULL,
  amount BIGINT NOT NULL, status TEXT NOT NULL);
```

**Seed data**

```sql
-- wallet_db
INSERT INTO wallets (user_id, available) VALUES (1, 0), (2, 0);
-- gateway_db
INSERT INTO tenant_balances VALUES ('ewallet', 0);
```

### 4.2 Flow A: Wallet Funding ($100 deposit, $3 fee, $1.50 gateway cut)

**A1. \[wallet_db\] Create the pending deposit and queue the charge request** (one local transaction, so the intent and the message can never diverge):

```sql
BEGIN;
INSERT INTO wallet_txns (idem_key, wallet_id, kind, amount, fee, status)
VALUES ('dep-001', 1, 'DEPOSIT', 10000, 300, 'PENDING')
ON CONFLICT (idem_key) DO NOTHING;           -- client retry = no-op
INSERT INTO outbox (topic, payload)
VALUES ('gateway.charge', '{"ref":"dep-001","amount":10300}');
COMMIT;
```

**A2. \[gateway_db\] Relay delivers the message; gateway charges the card and records the capture** (the inbox makes redelivery harmless):

```sql
BEGIN;
INSERT INTO inbox (event_id) VALUES ('charge:dep-001') ON CONFLICT DO NOTHING;
-- application checks: 0 rows inserted => duplicate, ROLLBACK and stop
INSERT INTO payments (provider_ref, tenant, amount, fee, status)
VALUES ('dep-001', 'ewallet', 10300, 150, 'CAPTURED');
INSERT INTO outbox (topic, payload)
VALUES ('wallet.webhook', '{"ref":"dep-001","status":"captured"}');
COMMIT;
```

**A3. \[wallet_db\] Webhook arrives: credit the wallet exactly once.** The status flip is the guard: only the first delivery matches `status='PENDING'`, so the credit cannot be applied twice even with concurrent duplicate webhooks.

```sql
BEGIN;
WITH t AS (
  UPDATE wallet_txns SET status = 'SUCCESS'
  WHERE idem_key = 'dep-001' AND status = 'PENDING'
  RETURNING wallet_id, amount, fee)
UPDATE wallets w SET available = w.available + t.amount
FROM t WHERE w.id = t.wallet_id;
-- only if 1 row was updated:
INSERT INTO outbox (topic, payload) VALUES ('ledger.post',
 '{"ref":"dep-001:capture","wallet":1,"receivable":10300,"wallet_credit":10000,"fee":300}');
COMMIT;
```

**A4. \[ledger_db\] Post the journal entry** (Debit 1210 $103.00 / Credit 2210 $100.00 / Credit 4210 $3.00). The unique `ref` makes it idempotent: a replay inserts no entry, so the CTE inserts no lines.

```sql
BEGIN;
WITH e AS (
  INSERT INTO journal_entries (ref, memo) VALUES ('dep-001:capture', 'Wallet funding')
  ON CONFLICT (ref) DO NOTHING RETURNING id)
INSERT INTO journal_lines (entry_id, account_code, wallet_id, debit, credit)
SELECT e.id, v.code, v.wid, v.dr, v.cr FROM e,
  (VALUES (1210, NULL::bigint, 10300, 0),
          (2210, 1,            0, 10000),
          (4210, NULL,         0, 300)) AS v(code, wid, dr, cr);
COMMIT;
```

**A5. \[gateway_db\] 24-48h later, bulk settlement** (net $101.50 sent to the corporate bank):

```sql
BEGIN;
UPDATE payments SET status = 'SETTLED'
WHERE provider_ref = 'dep-001' AND status = 'CAPTURED';
UPDATE tenant_balances SET available = available + 10150 WHERE tenant = 'ewallet';
INSERT INTO outbox (topic, payload) VALUES ('ledger.post',
 '{"ref":"dep-001:settle","cash":10150,"expense":150,"receivable":10300}');
COMMIT;
```

**A6. \[ledger_db\] Post the settlement** (Debit 1230 $101.50 / Debit 5210 $1.50 / Credit 1210 $103.00):

```sql
BEGIN;
WITH e AS (
  INSERT INTO journal_entries (ref, memo) VALUES ('dep-001:settle', 'Gateway settlement')
  ON CONFLICT (ref) DO NOTHING RETURNING id)
INSERT INTO journal_lines (entry_id, account_code, debit, credit)
SELECT e.id, v.code, v.dr, v.cr FROM e,
  (VALUES (1230, 10150, 0), (5210, 150, 0), (1210, 0, 10300)) AS v(code, dr, cr);
COMMIT;
```

### 4.3 Flow B: P2P Transfer ($25.00, wallet 1 to wallet 2)

Both wallets live in `wallet_db`, so this is one ordinary ACID transaction. Lock rows in a fixed order (lowest id first) so two opposite transfers (1 to 2 and 2 to 1) cannot deadlock.

```sql
-- [wallet_db]
BEGIN;
SELECT id FROM wallets WHERE id IN (1, 2) ORDER BY id FOR UPDATE;   -- deterministic lock order
UPDATE wallets SET available = available - 2500
WHERE id = 1 AND available >= 2500;      -- 0 rows => insufficient funds, ROLLBACK
UPDATE wallets SET available = available + 2500 WHERE id = 2;
INSERT INTO wallet_txns (idem_key, wallet_id, kind, amount, counterparty_wallet, status)
VALUES ('p2p-001', 1, 'P2P', 2500, 2, 'SUCCESS');
INSERT INTO outbox (topic, payload) VALUES ('ledger.post',
 '{"ref":"p2p-001","from":1,"to":2,"amount":2500}');
COMMIT;

-- [ledger_db] Debit 2210 (wallet 1) / Credit 2210 (wallet 2)
BEGIN;
WITH e AS (INSERT INTO journal_entries (ref, memo) VALUES ('p2p-001', 'P2P transfer')
           ON CONFLICT (ref) DO NOTHING RETURNING id)
INSERT INTO journal_lines (entry_id, account_code, wallet_id, debit, credit)
SELECT e.id, 2210, v.wid, v.dr, v.cr FROM e,
  (VALUES (1, 2500, 0), (2, 0, 2500)) AS v(wid, dr, cr);
COMMIT;
```

### 4.4 Flow C: VAS Utility Purchase ($10.00) as a Saga

**C1. \[wallet_db\] Place the hold.** The conditional `UPDATE` is the anti-double-spend guard: two concurrent purchases serialize on the row lock, and the second re-checks `available` after the first commits.

```sql
BEGIN;
UPDATE wallets SET available = available - 1000, held = held + 1000
WHERE id = 1 AND available >= 1000;       -- 0 rows => reject purchase
INSERT INTO wallet_txns (idem_key, wallet_id, kind, amount, status)
VALUES ('vas-001', 1, 'VAS', 1000, 'PENDING');
INSERT INTO outbox (topic, payload) VALUES ('ledger.post',
 '{"ref":"vas-001:hold","amount":1000}'),
 ('gateway.payout', '{"ref":"vas-001","amount":1000}');
COMMIT;
-- [ledger_db] ref 'vas-001:hold': Debit 2210 (wallet 1) 1000 / Credit 2220 1000
```

**C2a. Vendor API succeeded: \[wallet_db\] release the hold**

```sql
BEGIN;
WITH t AS (UPDATE wallet_txns SET status = 'SUCCESS'
           WHERE idem_key = 'vas-001' AND status = 'PENDING' RETURNING wallet_id, amount)
UPDATE wallets w SET held = w.held - t.amount FROM t WHERE w.id = t.wallet_id;
INSERT INTO outbox (topic, payload) VALUES ('ledger.post', '{"ref":"vas-001:final","amount":1000}');
COMMIT;
-- [ledger_db] ref 'vas-001:final': Debit 2220 1000 / Credit 1220 1000
```

**C2b. Vendor API failed: \[wallet_db\] compensating action** (refund the hold):

```sql
BEGIN;
WITH t AS (UPDATE wallet_txns SET status = 'FAILED'
           WHERE idem_key = 'vas-001' AND status = 'PENDING' RETURNING wallet_id, amount)
UPDATE wallets w SET held = w.held - t.amount, available = w.available + t.amount
FROM t WHERE w.id = t.wallet_id;
INSERT INTO outbox (topic, payload) VALUES ('ledger.post', '{"ref":"vas-001:release","amount":1000}');
COMMIT;
-- [ledger_db] ref 'vas-001:release': Debit 2220 1000 / Credit 2210 (wallet 1) 1000
```

### 4.5 Gateway Flows: Merchant Payment, Settlement, Payout

```sql
-- [gateway_db] Customer pays merchant $100.00, platform fee $2.80
BEGIN;
INSERT INTO payments (provider_ref, tenant, amount, fee, status)
VALUES ('pay-501', 'merchant_9', 10000, 280, 'CAPTURED');
INSERT INTO outbox (topic, payload) VALUES ('ledger.post', '{"ref":"pay-501:capture"}');
COMMIT;
-- [ledger_db] pay-501:capture: Debit 1110 10000 / Credit 2110 9720 / Credit 4110 280

-- [gateway_db] Card network settles ($1.10 interchange): merchant becomes withdrawable
BEGIN;
UPDATE payments SET status = 'SETTLED' WHERE provider_ref = 'pay-501' AND status = 'CAPTURED';
INSERT INTO tenant_balances (tenant, available) VALUES ('merchant_9', 9720)
ON CONFLICT (tenant) DO UPDATE SET available = tenant_balances.available + 9720;
INSERT INTO outbox (topic, payload) VALUES ('ledger.post', '{"ref":"pay-501:settle"}');
COMMIT;
-- [ledger_db] pay-501:settle: Debit 1120 9890 / Debit 5110 110 / Credit 1110 10000

-- [gateway_db] Merchant withdraws $97.20 (CHECK constraint blocks overdraw)
BEGIN;
UPDATE tenant_balances SET available = available - 9720
WHERE tenant = 'merchant_9' AND available >= 9720;   -- 0 rows => reject
INSERT INTO payouts (idem_key, tenant, amount, status)
VALUES ('wd-77', 'merchant_9', 9720, 'SENT') ON CONFLICT (idem_key) DO NOTHING;
INSERT INTO outbox (topic, payload) VALUES ('ledger.post', '{"ref":"wd-77"}');
COMMIT;
-- [ledger_db] wd-77: Debit 2110 9720 / Credit 1120 9720
```

---

## Part 5: Concurrency & Distributed Consistency

| Problem | Technique | Where in the SQL |
| --- | --- | --- |
| Two requests spend the same balance | Conditional update; row lock serializes writers; `CHECK (available >= 0)` as a backstop | Flows B, C1: `WHERE available >= amount` |
| Duplicate client retries or webhooks | Unique idempotency keys and compare-and-set on `status` | `idem_key`, `ON CONFLICT DO NOTHING`, `AND status='PENDING'` |
| Deadlock on opposite transfers | Lock rows in a fixed order | Flow B: `ORDER BY id FOR UPDATE` |
| DB commit succeeds but message is lost (no 2PC across DBs) | Transactional outbox: state change and message commit together | `INSERT INTO outbox` inside every business transaction |
| Message delivered twice | Idempotent consumers (inbox or unique journal `ref`) | A2 inbox; A4/A6 `ON CONFLICT (ref)` |
| Multi-step workflow fails midway | Saga with compensating transactions, not rollback | C2b release hold |

### 5.1 Outbox Relay Worker (safe to run many copies)

`SKIP LOCKED` lets parallel workers each claim different rows without blocking or double-sending:

```sql
BEGIN;
SELECT id, topic, payload FROM outbox
WHERE published_at IS NULL ORDER BY id LIMIT 50
FOR UPDATE SKIP LOCKED;
-- publish each row to the broker / target system, then:
UPDATE outbox SET published_at = now() WHERE id = ANY(:claimed_ids);
COMMIT;
```

If the worker crashes after publishing but before commit, the rows are re-sent. That is why every consumer must be idempotent (at-least-once delivery plus idempotency gives effectively-once).

### 5.2 Ledger Integrity Checks

```sql
-- [ledger_db] Every entry must balance (expect 0 rows)
SELECT entry_id FROM journal_lines
GROUP BY entry_id HAVING SUM(debit) <> SUM(credit);

-- Cross-system check: wallet_db total must equal ledger 2210 + 2220 balance
-- [wallet_db]
SELECT SUM(available) + SUM(held) AS wallet_total FROM wallets;
-- [ledger_db]
SELECT SUM(credit) - SUM(debit) AS ledger_total
FROM journal_lines WHERE account_code IN (2210, 2220);
```

The two totals can differ briefly while outbox events are in flight (eventual consistency). A reconciler should only flag a difference that persists past a grace window or that remains after the outbox has drained, then mark the affected transactions `DISCREPANCY_LOCKED` as in Part 3.

**Isolation level:** PostgreSQL's default READ COMMITTED is enough here, because correctness comes from conditional updates and unique constraints rather than from reads. Use `SERIALIZABLE` (with retry on error 40001) only for multi-row checks you can't express as a single conditional update.

---

## Part 6: Concurrency in Action: Two Purchases Race for One Balance

**Scenario:** wallet 1 has $50.00. Two sessions (S1, S2) each try to buy $30.00 at the same instant. Exactly one must succeed.

```
-- [wallet_db] setup
UPDATE wallets SET available = 5000, held = 0 WHERE id = 1;
```

### 6.1 The bug: read-modify-write in application code

```
-- S1 and S2 both run:
SELECT available FROM wallets WHERE id = 1;          -- both see 5000
-- app code: 5000 >= 3000, OK
UPDATE wallets SET available = 2000 WHERE id = 1;    -- both write 2000
```

Both purchases succeed but only $30 left the wallet: a **lost update** (DDIA ch.7). The bank just gave away $30. The fix is not 'be faster'; it is to make the check and the write one atomic statement.

### 6.2 The fix: conditional update, step by step

| Time | S1 | S2 | Row state |
| --- | --- | --- | --- |
| t1 | BEGIN; UPDATE wallets SET available = available - 3000, held = held + 3000 WHERE id = 1 AND available >= 3000; (1 row) |  | 2000 / held 3000, uncommitted, **row locked** |
| t2 |  | BEGIN; same UPDATE. **Blocks** waiting for S1's row lock. | locked |
| t3 | INSERT INTO wallet_txns (idem_key, ...) VALUES ('vas-A', ...); COMMIT; | still waiting | 2000 committed, lock released |
| t4 |  | Wakes up. Under READ COMMITTED, PostgreSQL re-evaluates the WHERE clause against the newest committed row: 2000 >= 3000 is false, so **0 rows**. App ROLLBACKs and returns INSUFFICIENT_FUNDS. | final: 2000 / held 3000 |

**Try it:** open two `psql` sessions, run t1 in one, t2 in the other, then t3. You can watch t2 hang until t3. The `CHECK (available >= 0)` constraint is the backstop: even a buggy query cannot make the balance negative.

**Idempotent retry:** if S2's client times out and retries with the same `idem_key`, the unique key on `wallet_txns` makes the retry return the original outcome instead of a second purchase.

---

## Part 7: Failure in Action

Networks drop, services restart, and a timeout is **ambiguous**: you cannot tell whether the request was lost or only the reply was (DDIA ch.8). So the design assumes every message can be delayed, duplicated or lost, and every step must be safe to repeat.

### 7.1 The ledger service is down mid-flow

A customer buys airtime (Flow C1) while `ledger_db` is unreachable. Nothing is lost: the wallet transaction already committed together with its outbox row, so the money is correctly held and the ledger message is waiting.

```
-- [wallet_db] monitoring query: alert when backlog or age grows
SELECT count(*) AS backlog, now() - min(created_at) AS oldest
FROM outbox WHERE published_at IS NULL;
```

The relay does not set `published_at` until the broker acknowledges delivery, and retries with exponential backoff plus jitter. When the ledger returns it posts the entries, and the totals from 5.2 converge. Until then the reconciler sees a temporary, explainable difference.

### 7.2 The ledger committed, but the acknowledgement was lost

The relay re-sends the same message. The ledger runs the same statement again; the unique `ref` makes `ON CONFLICT DO NOTHING` return no id, so the CTE inserts **zero lines**. Result: duplicate delivery, single posting. This is what turns at-least-once delivery into *effectively-once* processing.

### 7.3 Gateway timeout: charge status unknown

The deposit stays `PENDING`. A sweeper resolves it by asking the gateway using the **same reference** (never assume a timeout means failure):

```
-- [wallet_db] sweeper, safe to run on many workers
BEGIN;
SELECT idem_key, wallet_id, amount FROM wallet_txns
WHERE status = 'PENDING' AND created_at < now() - interval '15 minutes'
ORDER BY id LIMIT 20 FOR UPDATE SKIP LOCKED;
-- per row: query gateway by idem_key
--   captured  -> run step A3 (credit wallet)
--   not found -> mark FAILED (nothing to refund)
--   unknown   -> leave PENDING, retry next sweep
COMMIT;
```

### 7.4 Making the ledger tamper-resistant

```
-- [ledger_db] reject any entry whose debits and credits differ, checked at COMMIT
CREATE FUNCTION assert_balanced() RETURNS trigger AS $$
BEGIN
  IF (SELECT SUM(debit) - SUM(credit) FROM journal_lines
      WHERE entry_id = NEW.entry_id) <> 0 THEN
    RAISE EXCEPTION 'unbalanced journal entry %', NEW.entry_id;
  END IF;
  RETURN NULL;
END $$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_balanced AFTER INSERT ON journal_lines
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION assert_balanced();

-- the ledger is append-only: mistakes are fixed with a reversing entry, never an UPDATE
REVOKE UPDATE, DELETE ON journal_entries, journal_lines FROM app_role;
```

A message that fails this check is a *poison message*: after a few retries it goes to a dead-letter queue and pages a human, instead of blocking everything behind it.

---

## Part 8: Concept Map: Distributed and Fintech Ideas Used

| Concept | Why it matters (source) | Where used | Best practice in production |
| --- | --- | --- | --- |
| Money as integers | Floating point cannot represent 0.10 exactly, and rounding errors in money are bugs. | All amounts are BIGINT cents | Store minor units plus a currency code; define one rounding rule for fee splits and test it. |
| Double-entry and immutable ledger | Every movement balances, giving a built-in audit trail. Immutability: Helland, *Immutability Changes Everything*; event logs in DDIA ch.11. | Parts 1-2, `journal_lines` | Append-only, with reversing entries for corrections. Revoke UPDATE and DELETE; enforce balance in the database (7.4). |
| Transactions and isolation | Lost updates, write skew, and what READ COMMITTED vs SERIALIZABLE guarantee (DDIA ch.7). | Part 6, flows B and C1 | Prefer atomic conditional updates; use `FOR UPDATE` for multi-row invariants; on SERIALIZABLE, retry on SQLSTATE 40001 and deadlocks on 40P01. |
| Constraints as last line of defence | Uniqueness and non-negative balances must be enforced where the data lives (DDIA ch.12, enforcing constraints). | `CHECK`, `UNIQUE (ref)` | Never rely on application checks alone; partition by the constraint key (for example wallet id) when you shard. |
| Idempotency and the end-to-end argument | Retries are the only way to cope with ambiguity, so the operation itself must be repeat-safe (DDIA ch.11-12; Stripe's idempotency-key API design). | `idem_key`, `ref`, status compare-and-set | Client generates the key; server stores key with result; keys expire only after the retry window. |
| Unreliable networks and clocks | Timeouts are ambiguous; wall clocks drift (DDIA ch.8). | 7.3 sweeper | Use database sequences and status columns for ordering, not app timestamps. Query before assuming failure. |
| Dual-write problem and outbox | Writing to a DB and a broker separately can leave them inconsistent (DDIA ch.11, change data capture; Richardson, *Microservices Patterns*). | Every `outbox` insert | Publish via polling relay or log-based CDC (for example logical replication); keep outbox small by archiving published rows. |
| At-least-once plus idempotent consumers | Exactly-once delivery does not exist across systems; effectively-once is built from the two parts (DDIA ch.11). | Inbox, `ON CONFLICT` | Ack only after commit; dead-letter queue for poison messages; alert on backlog age. |
| Sagas instead of 2PC | Two-phase commit blocks on coordinator failure and couples availability (DDIA ch.9); sagas use compensations (Garcia-Molina and Salem, 1987; Helland, *Life Beyond Distributed Transactions*). | Flow C hold, release, compensate | Make each step and its compensation idempotent; model the saga state explicitly; compensations must not fail permanently. |
| Authorisation vs settlement | Card and gateway money moves in two phases: a hold (promise) then settlement (cash). Fintech convention. | 2220 holds, 1210 receivable, A5 settlement | Track pending and posted separately; hold expiry jobs; reconcile each phase. |
| Eventual consistency and reconciliation | Replicas and queues lag (DDIA ch.5); end-to-end correctness is verified by auditing, not assumed (DDIA ch.12). | Part 3, 5.2 | Run daily (or continuous) three-way reconciliation. Regulated firms must also keep and reconcile safeguarded customer funds. |
| Partitioning and ordering | Events for one key must be applied in order; different keys can run in parallel (DDIA ch.6, ch.11). | Relay and consumers | Partition topics by wallet id; shard databases by customer; keep all rows touched by one transaction on one shard. |
| Replication and durability | Failover can lose recently acknowledged writes unless replication is synchronous (DDIA ch.5). | All three databases | Synchronous standby for money databases, tested failover, point-in-time recovery backups. |
| Observability | You cannot debug a flow spread over three systems without correlation. | `ref` across all tables | Propagate one trace id from request to ledger entry; metrics for outbox lag, retries, and reconciliation breaks. |

---

## Part 9: From SQL to Code

Every handler in this manual is the same shape. Here it is as language-neutral pseudocode for the webhook step A3:

```
handleWebhook(event):
  tx = db.begin()
  if tx.exec(INSERT_INBOX, event.id) == 0:   // ON CONFLICT DO NOTHING: duplicate
      tx.rollback(); return ACK              // already handled, just acknowledge
  n = tx.exec(CAS_STATUS_AND_CREDIT, event.ref)   // the CTE from A3
  if n == 1: tx.exec(INSERT_OUTBOX, 'ledger.post', payload)
  tx.commit()
  return ACK                                 // ack only AFTER commit
on any error: return NACK, the broker redelivers (at-least-once)
```

**Rules to carry into any language**

- One local database transaction per service step; never hold one open across a network call.
- Acknowledge a message only after its transaction commits.
- Every write is idempotent (unique key, or compare-and-set on status).
- Retry serialization failures (40001) and deadlocks (40P01) with backoff and jitter; use integer money types.
- Test properties, not just examples: after any sequence of operations, every entry balances, wallet total equals ledger 2210 + 2220 once the outbox is drained, and no balance is negative.

**Suggested build order:** (1) schemas and migrations, (2) a ledger 'post entry' function with the 7.4 constraints, (3) wallet operations with a two-goroutine/thread race test like Part 6, (4) outbox relay with SKIP LOCKED, (5) a fake gateway that duplicates and drops messages, (6) the reconciler, (7) chaos tests that kill each service mid-flow.

**Further reading:** DDIA (ch.5-9, 11-12); Pat Helland, *Life Beyond Distributed Transactions* (CIDR 2007); Garcia-Molina and Salem, *Sagas* (1987); Chris Richardson, *Microservices Patterns* (outbox, saga); Stripe's engineering post on idempotency; Square's engineering post on its immutable double-entry ledger service; the PostgreSQL documentation chapters on concurrency control and explicit locking.