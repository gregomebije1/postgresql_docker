1. The wallet engine registers a customer A 
2. CustomerA funds his wallet 
3. customerA transfers money to another customerB. 
4. customerA buys data. 
5. CustomerA transfers money out from his wallet to his Bank Account. 
6. Wallet engine reconciles with Payment gateway.


## Workflow


```sql
--Phase Setup: Initial Chart of Accounts & Schemas
-- Before any workflow runs, the Ledger needs a Chart of Accounts (COA) to track assets, liabilities, and revenues properly.

-- ==========================================
-- 1. LEDGER SYSTEM DB (double-entry books for both systems)
-- The immutable core. Every financial movement writes 
-- at least two rows (one debit, one credit) to a journal entry.
-- ==========================================
CREATE TABLE ledger_db_accounts (
    code INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- ASSET, LIABILITY, REVENUE, EXPENSE
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_db_journal_entries (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    --status VARCHAR(50) NOT NULL DEFAULT 'COMMITTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- posted_at
);

CREATE TABLE ledger_db_journal_lines (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id),
    account_id INT NOT NUL REFERENCES accounts(id),
    wallet_id BIGINT,       -- sub-ledger tag for 2210
    debit BIGINT NOT NULL DEFAULT 0, 
    credit BIGINT NOT NULL DEFAULT 0,
    CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0))
);

-- ==========================================
-- 2. WALLET ENGINE DB (Fast Access Cache & State)
-- ==========================================
CREATE TABLE wallet_db_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallet_db_wallets (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    ledger_account_id UUID NOT NULL, -- Links to Ledger's liability account
    -- balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0),
    held      BIGINT NOT NULL DEFAULT 0 CHECK (held >= 0));
    currency VARCHAR(3) NOT NULL,
    version INT NOT NULL DEFAULT 0, -- Optimistic locking
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallet_db_wallet_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    type VARCHAR(50) NOT NULL, -- FUND, P2P, VAS, PAYOUT
    amount BIGINT NOT NULL, -- Amounts are integer pence
    fee BIGINT NOT NULL DEFAULT 0,
    counterparty_wallet BIGINT,
    status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED
    --reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallet_db_outbox (
  id BIGSERIAL PRIMARY KEY, 
  topic TEXT NOT NULL, 
  payload JSONB NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now(), 
  published_at TIMESTAMPTZ
);

CREATE TABLE wallet_db_inbox (
    event_id TEXT PRIMARY KEY, 
    received_at TIMESTAMPTZ DEFAULT now()
);


-- ==========================================
-- 3. PAYMENT GATEWAY DB (External Provider Simulation)
--The Payment Gateway operates as a clearing broker for merchant tenants. It interfaces directly with card networks and acquiring banks, processing gross receivables, extracting platform processing fees, and managing payout corridors.
-- ==========================================

CREATE TABLE gateway_db_tenant_balances (
  tenant TEXT PRIMARY KEY, 
  available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0)
);

CREATE TABLE gateway_db_payments (
  id BIGSERIAL PRIMARY KEY, 
  provider_ref TEXT UNIQUE NOT NULL, 
  tenant TEXT NOT NULL,
  amount BIGINT NOT NULL, 
  fee BIGINT NOT NULL,
  status TEXT NOT NULL -- CAPTURED | SETTLED
);         
  
CREATE TABLE gateway_db_payouts (
  id BIGSERIAL PRIMARY KEY, 
  idem_key TEXT UNIQUE NOT NULL, 
  tenant TEXT NOT NULL,
  amount BIGINT NOT NULL, 
  status TEXT NOT NULL
);

CREATE TABLE gateway_db_pg_transactions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL, -- CHARGE, PAYOUT
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED
    gateway_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ==========================================
-- SEEDING
-- Wallet Engine Registers Customer A and Customer B
-- and ledger System provisions their
-- corresponding liability accounts in the Ledger.
-- ==========================================

--[Wallet Engine DB]
BEGIN;
INSERT INTO wallet_db_users (id, email) VALUES ('user_uuid_A', 'customer.a@fintech.com');
-- Call [Ledger System DB] through an API to create account and get a ledger account ID
INSERT INTO wallet_db_wallets (id, user_id, ledger_account_id, available, currency) VALUES ('wal_uuid_A', 'user_uuid_A', 'wallet_db_acc_liability_cust_A', 0.0000, 'USD');
COMMIT;


BEGIN;
INSERT INTO wallet_db_users (id, email) VALUES ('user_uuid_B', 'customer.b@fintech.com');
-- Call [Ledger System DB] through an API to create account and get a ledger account ID
INSERT INTO wallet_db_wallets (id, user_id, ledger_account_id, available, currency) VALUES ('wal_uuid_B', 'user_uuid_B', 'wallet_db_acc_liability_cust_B', 0.0000, 'USD');
COMMIT;

-- [Ledger System DB] Create Customer A and B liability account in the ledger chart of accounts
INSERT INTO ledger_db_accounts (id, name, type, currency) 
VALUES ('wallet_db_acc_liability_cust_A', 'Customer A Liability Balance', 'LIABILITY', 'USD');
INSERT INTO ledger_db_accounts (id, name, type, currency) 
VALUES ('wallet_db_acc_liability_cust_B', 'Customer B Liability Balance', 'LIABILITY', 'USD');

-- [Ledger System DB] Create Chart of Accounts (COA) for Wallet Engine
INSERT INTO ledger_db_accounts VALUES
 (1210,'wallet_db_Gateway Funding Receivables','Asset'), -- Floating claims against the external gateway for incoming customer wallet deposits
 (1220,'wallet_db_Gateway Payout Clearing','Asset'), -- Transient tracking lane for outbound customer bank withdrawals routed through the gateway.
 (1230,'wallet_db_Settlement Bank Account','Asset'), -- Corporate bank account where the gateway deposits bulk payouts.   
 (2210,'wallet_db_User Wallet Balances (Liability Pool)','Liability'), -- The digital ledger balance leased to the user, reflecting their available spending power.
 (2220,'wallet_db_Saga Transaction Holds','Liability'),  -- Temporary escrow pool holding user funds during active multi-network workflows (e.g., utility purchases).
 (4210,'wallet_db_Commission Revenue','Revenue'), -- Margins earned by adding a convenience fee on top of the gateway's base pricing.
 (5210,'wallet_db_Gateway Processing Expense','Expense'); -- Interchange and processing costs deducted by the external provider


--==============
-- Pre-seed Ledger System Accounts (System Clearing Accounts)
--INSERT INTO accounts (id, name, type, currency) VALUES 
-- ('acc_gateway_clearing', 'Payment Gateway Clearing', 'ASSET', 'USD'),
-- ('acc_telco_revenue', 'Telco Data Revenue', 'REVENUE', 'USD'),
-- ('acc_bank_settlement', 'Bank Settlement Account', 'ASSET', 'USD');
--================

-- [Payment Gateway DB] Creating Wallet Engine as a Tenant
INSERT INTO gateway_db_tenant_balances VALUES ('ewallet', 0);

-- [Ledger System DB] Chart of Accounts (COA) for payment gateway
INSERT INTO accounts VALUES
 (1110,'gateway_db_Processor Receivables (In-Transit)','Asset'),   -- Funds captured by card networks but not yet deposited into the gateway's bank.    
 (1120,'gateway_db_Gateway Settlement Bank Account','Asset'), -- Corporate bank account used to receive network settlements and dispatch payouts.
 (2110,'gateway_db_Merchant Settlement Payables','Liability'), -- Net funds owed to your merchant tenants that are available for withdrawal. 
 (4110,'gateway_db_Gateway_Processing Fee Revenue','Revenue'), -- Gross margins earned by the platform from your variable/fixed transaction fee splits. 
 (5110,'gateway_db_Interchange/Processor Expense','Expense');  -- Upstream interchange fees charged by core Visa/Mastercard card networks or card factories.







-- ==========================================
-- Workflow A: Customer A Funds His Wallet
-- ($100 deposit, $3 fee, $1.50 gateway cut)
-- Objective: Customer A deposits $100 via Payment Gateway. Money moves from Gateway Clearing to Customer A's ledger account, and wallet balance updates.
-- ==========================================

-- Initiate: Client calls Wallet Engine to fund wallet.
-- **A1.  [Wallet Engine DB] Create the pending deposit and queue the charge request** (one local transaction, so the intent and the message can never diverge - Transactional Outbox pattern):

BEGIN;
INSERT INTO wallet_transactions (id, idempotency_key, wallet_id, type, amount, fee, status) VALUES ('wtx_001', 'dep-001', 1, 'FUND', 10000, 300, 'PENDING')ON CONFLICT (idem_key) DO NOTHING;           -- client retry = no-op
INSERT INTO outbox (topic, payload) VALUES ('gateway.charge', '{"ref":"dep-001","amount":10300}');
COMMIT;
```

-- Payment Gateway: Wallet Engine calls Payment Gateway to charge the card/bank. Payment Gateway returns a PENDING status or a redirect URL.

-- 1. [Payment Gateway DB] Initialize the charge transaction
INSERT INTO pg_transactions (id, idempotency_key, type, amount, currency, status, gateway_reference) 
VALUES ('pg_tx_101', 'idem_fund_001', 'CHARGE', 100.0000, 'USD', 'SUCCESS', 'ch_stripe_xyz123');


-- 2. [Wallet Engine DB] Log pending transaction and update wallet balance with pessimistic locking
BEGIN;

-- Lock wallet row to prevent concurrent updates
SELECT id, balance, version FROM wallets WHERE id = 'wal_uuid_A' FOR UPDATE;

--[delete]INSERT INTO wallet_transactions (id, idempotency_key, wallet_id, type, amount, status, reference_id) VALUES ('wtx_001', 'idem_fund_001', 'wal_uuid_A', 'FUND', 100.0000, 'SUCCESS', 'pg_tx_101');

UPDATE wallets 
SET balance = balance + 100.0000, version = version + 1, updated_at = CURRENT_TIMESTAMP 
WHERE id = 'wal_uuid_A';

-- Wallet Update: Wallet Engine updates the cached/fast-access balance or emits an event confirming the balance update.
COMMIT;

-- Webhook/Polling: Once the Payment Gateway confirms success (via webhook), the Wallet Engine triggers the Ledger System.


-- 3. [Ledger System DB] Record double-entry movement (Balanced: Debits = Credits)
-- Debit Asset (Gateway Clearing gets richer) | Credit Liability (We owe Customer A more money)
-- Ledger Post: Ledger records a double-entry transaction (Debit: Cash/Gateway Account, Credit: Customer A Liability Account).

BEGIN;

INSERT INTO journal_entries (id, idempotency_key, description) 
VALUES ('je_001', 'idem_fund_001', 'Wallet funding via Payment Gateway for Customer A');

INSERT INTO postings (id, journal_entry_id, account_id, amount, currency) VALUES 
('pst_001_1', 'je_001', 'acc_gateway_clearing', -100.0000, 'USD'), -- Debit
('pst_001_2', 'je_001', 'acc_liability_cust_A', 100.0000, 'USD');  -- Credit

COMMIT;

-- Compensating Actions: If the ledger post fails, the Saga triggers a refund/reversal API call to the Payment Gateway.

-- ==========================================
-- Workflow 3: Customer A Transfers Money to Customer B
-- Objective: Customer A sends $40 to Customer B. Must check sufficient balance, lock both wallets securely in a consistent order to prevent deadlocks, update balances, and post to the ledger.
-- ==========================================

-- Setup Customer B beforehand (Assumed registered with 'wal_uuid_B' and 'acc_liability_cust_B')
-- [Wallet Engine DB] Execute atomic transfer with strict locking order (by Wallet ID ascending)
BEGIN;

-- Lock both wallets in deterministic alphabetical order to prevent deadlocks
-- executes a transaction with a pessimistic lock to prevent negative balances:
SELECT id, balance FROM wallets WHERE id IN ('wal_uuid_A', 'wal_uuid_B') ORDER BY id ASC FOR UPDATE;

-- Validate balance in application logic (Customer A balance >= 40.00)
-- Update Customer A (Subtract)
UPDATE wallets 
SET balance = balance - 40.0000, version = version + 1 
WHERE id = 'wal_uuid_A';

-- Update Customer B (Add)
UPDATE wallets 
SET balance = balance + 40.0000, version = version + 1 
WHERE id = 'wal_uuid_B';

-- Record audit trail transactions
INSERT INTO wallet_transactions (id, idempotency_key, wallet_id, type, amount, status, reference_id) VALUES 
('wtx_002_out', 'idem_transfer_002', 'wal_uuid_A', 'TRANSFER', -40.0000, 'SUCCESS', 'wal_uuid_B'),
('wtx_002_in', 'idem_transfer_002_b', 'wal_uuid_B', 'TRANSFER', 40.0000, 'SUCCESS', 'wal_uuid_A');

COMMIT;

-- Simultaneously emit a request to Ledger to record the balanced posting.
-- [Ledger System DB] Internal liability shift (Zero-sum ledger entry)
BEGIN;

INSERT INTO journal_entries (id, idempotency_key, description) 
VALUES ('je_002', 'idem_transfer_002', 'Peer-to-peer transfer from Customer A to Customer B');

INSERT INTO postings (id, journal_entry_id, account_id, amount, currency) VALUES 
('pst_002_1', 'je_002', 'acc_liability_cust_A', -40.0000, 'USD'), -- Debit Customer A Liability
('pst_002_2', 'je_002', 'acc_liability_cust_B', 40.0000, 'USD');  -- Credit Customer B Liability

COMMIT;

-- ==========================================
-- Workflow 4: Customer A Buys Data
-- Objective: Customer A purchases a $15 data bundle. Money leaves Customer A's wallet and routes to Telco Revenue.
-- ==========================================


-- 1. [Wallet Engine DB] Deduct funds for data purchase
BEGIN;

SELECT id, balance FROM wallets WHERE id = 'wal_uuid_A' FOR UPDATE;

-- Check balance >= 15.00 in app logic
UPDATE wallets 
SET balance = balance - 15.0000, version = version + 1 
WHERE id = 'wal_uuid_A';

INSERT INTO wallet_transactions (id, idempotency_key, wallet_id, type, amount, status, reference_id) 
VALUES ('wtx_003', 'idem_data_003', 'wal_uuid_A', 'DATA_PURCHASE', -15.0000, 'SUCCESS', 'TELCO_BUNDLE_5GB');

COMMIT;

-- 2. [Ledger System DB] Record revenue generation
BEGIN;

INSERT INTO journal_entries (id, idempotency_key, description) 
VALUES ('je_003', 'idem_data_003', 'Data bundle purchase for Customer A');

INSERT INTO postings (id, journal_entry_id, account_id, amount, currency) VALUES 
('pst_003_1', 'je_003', 'acc_liability_cust_A', -15.0000, 'USD'), -- Debit Customer A Liability (Decrease liability)
('pst_003_2', 'je_003', 'acc_telco_revenue', 15.0000, 'USD');     -- Credit Telco Revenue (Recognize revenue)

COMMIT;

-- ==========================================
-- Workflow 5: Customer A Transfers Money Out to His Bank Account
-- Objective: Customer A withdraws $20 to their external bank account via the Payment Gateway.
-- ==========================================

-- 1. [Payment Gateway DB] Initiate external payout order
INSERT INTO pg_transactions (id, idempotency_key, type, amount, currency, status, gateway_reference) 
VALUES ('pg_tx_501', 'idem_payout_005', 'PAYOUT', 20.0000, 'USD', 'PENDING', 'po_stripe_bank789');

-- 2. [Wallet Engine DB] Hold/Deduct funds from Customer A's wallet
BEGIN;

SELECT id, balance FROM wallets WHERE id = 'wal_uuid_A' FOR UPDATE;

UPDATE wallets 
SET balance = balance - 20.0000, version = version + 1 
WHERE id = 'wal_uuid_A';

INSERT INTO wallet_transactions (id, idempotency_key, wallet_id, type, amount, status, reference_id) 
VALUES ('wtx_004', 'idem_payout_005', 'wal_uuid_A', 'PAYOUT', -20.0000, 'PENDING', 'pg_tx_501');

COMMIT;

-- 3. [Ledger System DB] Move funds to Bank Settlement Clearing
BEGIN;

INSERT INTO journal_entries (id, idempotency_key, description) 
VALUES ('je_004', 'idem_payout_005', 'Bank withdrawal payout initiated for Customer A');

INSERT INTO postings (id, journal_entry_id, account_id, amount, currency) VALUES 
('pst_004_1', 'je_004', 'acc_liability_cust_A', -20.0000, 'USD'), -- Debit Customer A Liability
('pst_004_2', 'je_004', 'acc_bank_settlement', 20.0000, 'USD');   -- Credit Bank Settlement Asset clearing
COMMIT;

-- ==========================================
-- Workflow 6: Wallet Engine Reconciles with Payment Gateway
-- Objective: Automated reconciliation engine runs a daily or hourly job comparing internal transaction logs against the -- Payment Gateway's statement to find discrepancies (e.g., ghost transactions, dropped webhooks, fee mismatches).
-- ==========================================

-- 1. Create a reconciliation audit table in Ledger or Wallet DB
CREATE TABLE reconciliation_runs (
    id UUID PRIMARY KEY,
    run_date DATE NOT NULL,
    total_pg_records INT,
    total_internal_records INT,
    status VARCHAR(50) NOT NULL, -- MATCHED, DISCREPANCY_FOUND
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reconciliation_exceptions (
    id UUID PRIMARY KEY,
    reconciliation_run_id UUID REFERENCES reconciliation_runs(id),
    transaction_id VARCHAR(255),
    issue_type VARCHAR(100), -- MISSING_IN_INTERNAL, AMOUNT_MISMATCH
    resolved BOOLEAN DEFAULT FALSE
);

-- 2. [SQL Reconciliation Query] Detect discrepancies between Payment Gateway DB and Wallet/Ledger records
-- (In microservices, this would query a data warehouse or run via Foreign Data Wrappers / API aggregation)
SELECT 
    pg.id AS pg_tx_id,
    pg.amount AS pg_amount,
    wt.amount AS wallet_amount,
    CASE 
        WHEN wt.id IS NULL THEN 'MISSING_IN_WALLET_ENGINE'
        WHEN pg.amount <> ABS(wt.amount) THEN 'AMOUNT_MISMATCH'
        ELSE 'MATCHED'
    END AS reconciliation_status
FROM pg_transactions pg
FULL OUTER JOIN wallet_transactions wt ON pg.id = wt.reference_id
WHERE pg.created_at >= CURRENT_DATE - INTERVAL '1 day';

-- 3. Log the reconciliation run result
INSERT INTO reconciliation_runs (id, run_date, total_pg_records, total_internal_records, status) 
VALUES ('rec_run_2026_03_30', '2026-03-30', 2, 2, 'MATCHED');
```

## Two-Phase Commit (2PC) 
is a protocol that ensures a group of separate databases either all save a transaction or all cancel it together. Think of it like a wedding ceremony where a coordinator asks everyone if they agree before making things official.
- Phase 1 (The Ask): A central coordinator asks every database, "Are you ready to save this data?" Each database checks its work, locks the data, and replies Yes or No.
- Phase 2 (The Decision): If everyone says Yes, the coordinator tells everyone to Commit (save) the data. If even one database says No, the coordinator tells everyone to Rollback (undo) the data. (rollback)
- The Good: It guarantees strict consistency. You never end up with a half-finished transaction, like money leaving one bank account without arriving in another.
- The Bad: It is slow and fragile. If the coordinator crashes after the first step, the databases stay stuck waiting for instructions and lock up the data.
- Consistency Model	Strong (ACID): Data is immediately consistent across all services.
- Resource Locking	Synchronous & Blocking: Locks database rows across services until everyone finishes.


## Saga
A Saga breaks a wallet top-up into three separate steps across your Wallet, Gateway, and Ledger systems, saving each step immediately instead of locking everything at once. If a later step fails, the system triggers custom "undo" actions to keep the customer's money accurate.

The Happy Path (Everything Works)

• 1. Wallet System: Creates a "Pending" top-up record for £50 and passes the request forward.
• 2. Gateway System: Charges the customer's bank card £50 and confirms the payment succeeded.
• 3. Ledger System: Records the £50 credit in the accounting books and tells the Wallet System to mark the balance as "Completed."

When Something Fails (The Undo Step)

• The Failure: Imagine the Gateway successfully charges the customer's card, but the Ledger System crashes or rejects the entry.
• The Compensation: Because the Gateway cannot simply "un-charge" a card automatically, the Saga triggers a compensating transaction (an undo action). The Gateway issues a £50 refund to the card, and the Wallet System updates the status to "Failed."

Why This Works Well for Wallets

• No Locked Accounts: Your Gateway doesn't hold up the Ledger, and slow external bank networks won't freeze your internal database.
• Clear Audit Trail: Every step—including the refund—is saved as its own distinct event in your records.

- Consistency Model: Eventual (BASE): Data becomes consistent after all steps finish.
- Resource Locking: Asynchronous & Non-Blocking: No distributed locks. Each service commits immediately.



## Double-Entry Bookkeeping: 
Every transaction consists of balanced debits and credits. Money is never created or destroyed; it moves from one account to another.



## Strong (ACID) vs. Eventual (BASE) Consistency

• Strong Consistency (ACID): Every system updates at the exact same millisecond. If a user checks their Wallet the instant the Gateway charges their card, the Ledger already has the entry. Nothing is visible until every step finishes.
• Eventual Consistency (BASE): Systems update one by one, leaving a brief delay before everything matches. For a second or two, the Gateway shows the £50 charge, while the Wallet still says "Pending" and the Ledger hasn't recorded it yet. Once all messages finish processing, all three systems match.



## Coordinating a Saga: Choreography vs. Orchestration

There are two ways to manage the workflow across your three systems:
• Choreography (No Central Boss): Each service listens for events and decides what to do next on its own.
	• Example: The Wallet publishes a TopUpRequested event. The Gateway hears this, charges the card, and publishes PaymentSucceeded. The Ledger hears that and records the credit.
• Orchestration (One Central Manager): A dedicated coordinator (the Orchestrator) tells each service what to do and when.
	• Example: A central "Top-Up Manager" sends a direct command to the Gateway to charge the card. Once the Gateway replies, the Manager commands the Ledger to record the credit, then tells the Wallet to update the balance.

## Handling Lack of Isolation (Preventing "Dirty Reads")

Because Sagas save changes right away without locking, a customer might see or spend unconfirmed money. You can prevent this with two simple design patterns:
• Semantic Lock (Status Flags): Mark records as PENDING until the entire Saga finishes. If the Wallet marks the £50 deposit as PENDING, the app knows not to let the user withdraw or spend those funds until the Ledger confirms and flips the status to COMPLETED.
• Pessimistic View (Reorder Steps): Put the riskiest steps first so users never see false data. For example, charge the Gateway before showing the £50 in the Wallet, rather than crediting the Wallet first and hoping the Gateway succeeds.

## Preventing Double-Charging (Idempotency Keys)

Networks glitch, and users often tap "Pay" twice. Idempotency ensures that repeating the exact same request only executes the action once.
• How it works: When the customer initiates the £50 top-up, the Wallet system generates a unique ID (e.g., txn_98765) called an idempotency key and sends it to the Gateway.
• The Result: If the Wallet retries the request because of a slow network, the Gateway sees txn_98765 in its history, skips charging the card a second time, and simply returns the original "Success" receipt.

- Every mutating API request (fund, transfer, buy data) must include an Idempotency-Key header. Services store processed keys in a fast store (Redis/DB with a unique constraint) to safely retry failed network requests without double-charging or double-crediting.


## The Transactional Outbox Pattern (Safe Event Publishing)

• The Problem: When the Gateway charges £50, it needs to save the payment in its database and send a PaymentSucceeded message to the Ledger. If the database saves the charge but the server crashes before sending the message, the Ledger never finds out and the customer loses £50.
• The Solution: Save the message inside the same database as the payment, then let a background worker send it later.
	• Step 1: In one single local database transaction, the Gateway saves the £50 payment record and writes the PaymentSucceeded event into a special Outbox table. Either both save, or neither saves.
	• Step 2: A separate background process constantly reads new rows from the Outbox table, publishes them to your message broker (like Kafka or RabbitMQ), and marks them as sent.
- Transactional Outbox Pattern: To reliably publish events from the Wallet Engine to the Ledger without dual-write issues, save the event to an outbox table within the same local database transaction, then poll/stream it to a message broker (Kafka/RabbitMQ).

## Handling Stuck or Timed-Out Transactions

• The Problem: The Wallet asks the Gateway to charge £50, and the network times out. The Wallet has no idea if the Gateway crashed before charging the card, or charged the card and crashed before replying.
• The Solution: Combine Timeouts, Status Polling, and Retries using your idempotency key (txn_98765).
	• Step 1 (Wait & Mark): If the Gateway does not reply within 10 seconds, the Wallet marks the top-up as UNKNOWN or VERIFYING instead of assuming it failed.
	• Step 2 (Reconcile / Poll): A background job periodically asks the Gateway, "What happened to txn_98765?"
		• If the Gateway has no record of it, the job safely retries the charge using the same idempotency key.
		• If the Gateway confirms it succeeded, the job pushes the Saga forward to the Ledger.
		• If the Gateway confirms it failed, the job marks the Wallet top-up as FAILED.

- Timeouts: Always apply connection and read timeouts on your HttpClient layer underlying WebClient to prevent threads from hanging indefinitely if the Ledger or PG goes down.


## Out-of-Order Events

• The Problem: Message brokers do not always deliver events in the exact order they happened. For example, a customer requests a £50 top-up and immediately cancels it, or a PaymentRefunded webhook arrives at the Ledger before the PaymentSucceeded event due to network delays.
• The Solution: Attach a version number or logical timestamp to every transaction event. Design each service as a strict state machine so the Ledger ignores or delays a refund event if it has not yet processed the initial deposit.

## Race Conditions & Concurrent Balance Updates

• The Problem: Two actions happen to the same wallet at the exact same time—such as a £50 top-up arriving while the user spends £30. Without protection, both processes might read the same starting balance (£100) and overwrite each other, losing one of the updates.
• The Solution: Use Optimistic Locking (adding a version column to the wallet row so updates fail if the version changed) or Pessimistic Locking (SELECT FOR UPDATE) on the specific wallet record during balance calculations.

Pessimistic Locking (PESSIMISTIC_WRITE): In Spring Data JPA, use @Lock(LockModeType.PESSIMISTIC_WRITE) on repository methods fetching account balances. This acquires an exclusive row-level lock (SELECT ... FOR UPDATE) in PostgreSQL, forcing concurrent threads to queue up.

Optimistic Locking (@Version): Add a @Version column to the Wallet/Account entity. If two threads read version 1 and try to update, the second thread throws an OptimisticLockException, triggering a retry or failure. Best used when contention is low.

Isolation Levels: Ensure database transactions use ISOLATION_REPEATABLE_READ or ISOLATION_SERIALIZABLE where strict monetary accuracy is required.

## Poison Pill Messages (Infinite Retry Loops)

• The Problem: A corrupted or malformed event (e.g., an invalid currency code from the Gateway) lands in your queue. The Ledger tries to process it, crashes, and the broker automatically retries it forever—blocking all valid top-ups behind it.
• The Solution: Set a maximum retry limit (e.g., 3 attempts) and route failing messages to a Dead Letter Queue (DLQ). This clears the lane for healthy transactions while alerting engineers to inspect the broken message.

## Dual-Write Drift & Reconciliation Gaps

• The Problem: Even with Outbox patterns and retries, external bank networks can quietly reverse a charge days later, or subtle bugs can cause the Wallet balance to drift away from the Ledger's accounting records.
• The Solution: Build an automated Reconciliation Engine. Every night, a batch job compares raw settlement files from the Gateway against your Ledger entries and Wallet balances, flagging any discrepancies for automated or manual resolution.
- Reconciliation: An automated nightly or real-time background job that compares the internal Ledger balances against external Payment Gateway settlement reports to detect discrepancies.


## Cascading Failures (Retry Storms)

• The Problem: If the Ledger slows down under heavy load, the Wallet and Gateway might aggressively retry failed requests simultaneously. This flood of retries completely overwhelms the recovering Ledger and takes down the whole platform.
• The Solution: Implement Circuit Breakers (which temporarily pause calls to a struggling service) and Exponential Backoff with Jitter (waiting progressively longer, randomized intervals between retries). Circuit Breakers: Wrap calls to external providers (like the Payment Gateway) in Resilience4j circuit breakers to fail fast and protect system resources during downstream outages.


## Clock Drift Across Servers

• The Problem: The servers running your Wallet, Gateway, and Ledger will inevitably have slightly different system clocks (even off by a few hundred milliseconds). Relying on local server timestamps to decide which transaction happened first will corrupt your audit trail.
• The Solution: Never use local server clocks for transaction ordering. Rely on a single authoritative source for sequence numbers (like the Ledger's database auto-incrementing ID) or use monotonically increasing event versions.

