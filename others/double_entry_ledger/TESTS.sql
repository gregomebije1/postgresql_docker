-- ============================================================================
-- ledger_manual_demo_debit_credit.sql  --  run ONE STEP AT A TIME, in order.
-- Requires: ledger_schema_debit_credit.sql loaded first.
--
-- Convention
--   Every line has a `debit` and a `credit` column (integer minor units, e.g. cents).
--   Exactly ONE of them is > 0; the other is 0.
--
-- How the tests work
--   The balance trigger is DEFERRED, so it only checks when the transaction
--   COMMITs. Every test looks like:
--       BEGIN;  insert header;  insert lines;  COMMIT;
--   If a rule is broken you get an ERROR and EVERYTHING is undone.
--   If your client says "current transaction is aborted", run ROLLBACK;
--
--   "Gap" tests use SET CONSTRAINTS ALL IMMEDIATE (= run the check now) and end
--   with ROLLBACK, so bad data is never kept. No error there = a hole in the schema.
--
--   The ledger is append-only, so you cannot DELETE test data. To start over:
--   DROP SCHEMA public CASCADE; CREATE SCHEMA public;  and reload the schema.
-- ============================================================================


-- ############################################################################
-- STEP 0: two tenants and some accounts
-- ############################################################################
INSERT INTO tenants (id, name) VALUES ('tenant-a', 'Tenant A'), ('tenant-b', 'Tenant B');

INSERT INTO accounts (id, tenant_id, account_number, name, type, currency) VALUES
 ('a1000000-0000-0000-0000-000000000001', 'tenant-a', '1001', 'Alice wallet',   'ASSET',  'USD'),
 ('a1000000-0000-0000-0000-000000000002', 'tenant-a', '1002', 'Bob wallet',     'ASSET',  'USD'),
 ('a1000000-0000-0000-0000-000000000003', 'tenant-a', '3001', 'Equity (USD)',   'EQUITY', 'USD'),
 ('a1000000-0000-0000-0000-000000000004', 'tenant-a', '1003', 'GBP wallet',     'ASSET',  'GBP'),
 ('b1000000-0000-0000-0000-000000000001', 'tenant-b', '1001', 'B wallet',       'ASSET',  'USD'),
 ('b1000000-0000-0000-0000-000000000002', 'tenant-b', '3001', 'B equity',       'EQUITY', 'USD');

SELECT tenant_id, account_number, name, type, currency FROM accounts ORDER BY tenant_id, account_number;

-- ############################################################################
-- STEP 1: a VALID posting. Fund Alice with 1000.00 (100000 cents)
--   Dr Alice 100000   /   Cr Equity 100000
-- EXPECT: succeeds. Run the two look-at-it queries.
-- ############################################################################
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-000000000001', 'tenant-a', 'Fund Alice', 'fund-alice-1');

INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 100000,      0),
 ('tenant-a', 'f0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000003',      0, 100000);
COMMIT;


-- ############################################################################
-- STEP 2: Alice pays Bob 25.00
--   Dr Bob 2500   /   Cr Alice 2500
-- EXPECT: succeeds. Alice 97500, Bob 2500.
-- ############################################################################
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-000000000002', 'tenant-a', 'Alice pays Bob', 'pay-bob-1');

INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002', 2500,    0),
 ('tenant-a', 'f0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001',    0, 2500);
COMMIT;


-- ############################################################################
-- INVARIANT 1: every transaction must balance (per currency)
-- ############################################################################

-- 1a. UNBALANCED: debit 2500 vs credit 2000
-- EXPECT: ERROR at COMMIT  "is unbalanced in currency USD: debits 2500 <> credits 2000"
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a1', 'tenant-a', 'Unbalanced', 'test-unbalanced');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a1', 'a1000000-0000-0000-0000-000000000002', 2500,    0),
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a1', 'a1000000-0000-0000-0000-000000000001',    0, 2000);
COMMIT;
ROLLBACK;   -- only needed if your client says the transaction is aborted

-- 1a-check: the header must NOT exist (all-or-nothing). EXPECT: 0 rows
SELECT * FROM ledger_transactions WHERE correlation_id = 'test-unbalanced';


-- 1b. ONLY ONE LINE
-- EXPECT: ERROR at COMMIT  "must contain at least 2 entries"
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a2', 'tenant-a', 'One line', 'test-one-line');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a2', 'a1000000-0000-0000-0000-000000000001', 100, 0);
COMMIT;
ROLLBACK;


-- 1c. MALFORMED LINES (new with two columns). Run each block separately.
--     All three: EXPECT immediate ERROR "violates check constraint"

-- 1c-i. a line that is BOTH debit and credit
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a3', 'tenant-a', 'Both sides', 'test-both-sides');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a3', 'a1000000-0000-0000-0000-000000000001', 100, 100);   -- ck_entry_one_side

ROLLBACK;

-- 1c-ii. a line that is NEITHER (0 / 0)
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a6', 'tenant-a', 'Zero line', 'test-zero');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a6', 'a1000000-0000-0000-0000-000000000001', 0, 0);       -- ck_entry_one_side
ROLLBACK;

-- 1c-iii. a NEGATIVE debit (someone smuggling the old signed style back in)
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a7', 'tenant-a', 'Negative', 'test-negative');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a7', 'a1000000-0000-0000-0000-000000000001', -100, 0);    -- ledger_entries_debit_check
ROLLBACK;


-- 1d. USD debit "balanced" by a GBP credit 
-- EXPECT: ERROR  "is unbalanced in currency GBP: debits 0 <> credits 100"  (or USD, whichever it hits first)
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a4', 'tenant-a', 'USD vs GBP', 'test-cross-currency');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a4', 'a1000000-0000-0000-0000-000000000001', 100,   0),   -- USD
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a4', 'a1000000-0000-0000-0000-000000000004',   0, 100);  -- GBP
SET CONSTRAINTS ALL IMMEDIATE; -- verifies that all data rules are satisfied immediately
ROLLBACK;


-- 1d-ok. A multi-currency transaction where EACH currency balances is allowed.
-- EXPECT: no error at SET CONSTRAINTS (4 lines, USD 100/100 and GBP 100/100). Then we discard it.
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a8', 'tenant-a', 'Two currencies, each balanced', 'test-two-currencies');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a8', 'a1000000-0000-0000-0000-000000000002', 100,   0),   -- USD Dr
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a8', 'a1000000-0000-0000-0000-000000000003',   0, 100),   -- USD Cr
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a8', 'a1000000-0000-0000-0000-000000000004', 100,   0),   -- GBP Dr
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000a8', 'a1000000-0000-0000-0000-000000000004',   0, 100);  -- GBP Cr (same account, nets out)
SET CONSTRAINTS ALL IMMEDIATE;
SELECT 'accepted: each currency balances' AS result;
ROLLBACK;


-- 1e. GAP (still open): a transaction header with NO lines at all
-- EXPECT (should): ERROR.   ACTUAL: no error, the empty header is accepted.
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000a5', 'tenant-a', 'Empty', 'test-empty');
SET CONSTRAINTS ALL IMMEDIATE;
SELECT count(*) AS lines_in_this_tx FROM ledger_entries
WHERE transaction_id = 'f0000000-0000-0000-0000-0000000000a5';   -- 0, yet no error
ROLLBACK;


-- ############################################################################
-- INVARIANT 2: a posting is atomic (header + lines together or not at all)
-- ############################################################################
-- Second line points at an account that does not exist.
-- EXPECT: ERROR (foreign key). Everything is undone.
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000b1', 'tenant-a', 'Atomic test', 'test-atomic');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000b1', 'a1000000-0000-0000-0000-000000000001',   0, 100),
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000b1', '99999999-9999-9999-9999-999999999999', 100,   0);
COMMIT;
ROLLBACK;

-- Check: EXPECT 0 rows (no orphan header left behind)
SELECT * FROM ledger_transactions WHERE correlation_id = 'test-atomic';


-- ############################################################################
-- INVARIANT 3: posted history is immutable
-- ############################################################################

-- 3a. Try to change a posted line.   EXPECT: ERROR "Immutability Violation"
UPDATE ledger_entries SET debit = 999999
WHERE transaction_id = 'f0000000-0000-0000-0000-000000000002' AND debit > 0;

-- 3b. Try to delete a posted line.   EXPECT: ERROR "Immutability Violation"
DELETE FROM ledger_entries WHERE transaction_id = 'f0000000-0000-0000-0000-000000000002';

-- 3c. Try to edit / delete a header. EXPECT: ERROR "Immutability Violation" (both)
UPDATE ledger_transactions SET description = 'tampered' WHERE id = 'f0000000-0000-0000-0000-000000000002';
DELETE FROM ledger_transactions WHERE id = 'f0000000-0000-0000-0000-000000000002';

-- 3d. GAP (still open): TRUNCATE is not stopped by row-level triggers.
-- EXPECT (should): ERROR.   ACTUAL: the table is emptied (shown as 0), then we ROLLBACK.
BEGIN;
TRUNCATE ledger_entries;
SELECT count(*) AS entries_left FROM ledger_entries;   -- 0  <-- history wiped
ROLLBACK;
SELECT count(*) AS entries_after_rollback FROM ledger_entries;   -- back to normal

-- 3e. The RIGHT way to correct: a NEW reversing transaction (undo step 2).
-- With two columns, a reversal is simply "swap debit and credit" on each original line.
-- EXPECT: succeeds. Alice back to 100000, Bob back to 0, original lines untouched.
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-000000000003', 'tenant-a', 'Reversal of pay-bob-1', 'reverse-pay-bob-1');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002',    0, 2500),  -- was Dr 2500
 ('tenant-a', 'f0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001', 2500,    0);  -- was Cr 2500
COMMIT;
-- (the schema has no column linking a reversal to its original; only the description does)
-- Run the "look at the ledger" and balance queries again.


-- ############################################################################
-- INVARIANT 4: integer money
-- ############################################################################

-- 4a. Column types.  EXPECT: debit = bigint, credit = bigint
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'ledger_entries' AND column_name IN ('debit', 'credit');

-- 4b. GAP (still open): what happens to 10.5?  EXPECT (should): ERROR.  ACTUAL: stored as 11.
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000c1', 'tenant-a', 'Fraction', 'test-fraction');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit)
VALUES ('tenant-a', 'f0000000-0000-0000-0000-0000000000c1', 'a1000000-0000-0000-0000-000000000001', 10.5, 0);
SELECT debit FROM ledger_entries WHERE transaction_id = 'f0000000-0000-0000-0000-0000000000c1';   -- 11
ROLLBACK;
-- Lesson: BIGINT gives integers, but the DB will not reject fractions.
-- Your API layer must reject non-integer amounts.


-- ############################################################################
-- INVARIANT 6: tenant isolation
-- ############################################################################

-- 6a. GAP (still open): a tenant-a posting that touches a tenant-b account.
-- EXPECT (should): ERROR.   ACTUAL: accepted, because the FK only checks that the account id exists.
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000d1', 'tenant-a', 'Cross tenant', 'test-cross-tenant');
INSERT INTO ledger_entries (tenant_id, transaction_id, account_id, debit, credit) VALUES
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000d1', 'a1000000-0000-0000-0000-000000000001',   0, 100),
 ('tenant-a', 'f0000000-0000-0000-0000-0000000000d1', 'b1000000-0000-0000-0000-000000000001', 100,   0);
SET CONSTRAINTS ALL IMMEDIATE;      -- no error
SELECT e.tenant_id AS entry_tenant, a.tenant_id AS account_tenant, e.debit, e.credit
FROM ledger_entries e JOIN accounts a ON a.id = e.account_id
WHERE e.transaction_id = 'f0000000-0000-0000-0000-0000000000d1';   -- tenant-a entry on a tenant-b account
ROLLBACK;

-- 6b. GAP (still open): nothing stops one tenant reading another's data.
-- EXPECT (should): a tenant-a session sees only tenant-a rows.  ACTUAL: you see both.
SELECT tenant_id, name FROM accounts;
-- Isolation only exists if EVERY query remembers "WHERE tenant_id = ..."; there is no row-level security yet.


-- ############################################################################
-- INVARIANT 7: retries / idempotency  (only what the current schema offers)
-- ############################################################################

-- 7a. Repeat the SAME correlation_id in the same tenant.
-- EXPECT: ERROR "duplicate key ... uq_transactions_tenant_correlation"
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000e1', 'tenant-a', 'Retry of fund', 'fund-alice-1');
ROLLBACK;

-- 7b. Same key, DIFFERENT tenant is fine.   EXPECT: succeeds (then rolled back)
BEGIN;
INSERT INTO ledger_transactions (id, tenant_id, description, correlation_id)
VALUES ('f0000000-0000-0000-0000-0000000000e2', 'tenant-b', 'Same key other tenant', 'fund-alice-1');
SELECT tenant_id, correlation_id FROM ledger_transactions WHERE correlation_id = 'fund-alice-1';
ROLLBACK;

-- Limits of what you just saw:
--   * the retry gets an ERROR, not the ORIGINAL result back
--   * a different payload under the same key is not compared, just "duplicate key"
--   * there is no idempotency_records table yet (the commented-out stub in the schema)


-- ############################################################################
-- INVARIANT 5: holds / available balance
-- ############################################################################
-- Not testable yet: account_holds does not exist (commented-out stub).


-- ############################################################################
-- FINAL: materialized view vs the real ledger
-- ############################################################################
SELECT * FROM mv_account_balances ORDER BY tenant_id;      -- EXPECT: empty / stale until refreshed
REFRESH MATERIALIZED VIEW mv_account_balances;
SELECT m.tenant_id, a.name, m.total_debits, m.total_credits, m.snapshot_balance
FROM mv_account_balances m JOIN accounts a ON a.id = m.account_id
ORDER BY m.tenant_id, a.name;
-- snapshot_balance is debit-positive: Alice +100000, Equity -100000.
-- Per tenant, SUM(total_debits) must equal SUM(total_credits):
SELECT tenant_id, SUM(total_debits) AS debits, SUM(total_credits) AS credits
FROM mv_account_balances GROUP BY tenant_id;