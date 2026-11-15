-- ============================================================================
-- 1. BASE LAYER EXTENSIONS & ENUMS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; 

CREATE TYPE compliance_tier AS ENUM ('TIER_1', 'TIER_2', 'TIER_3');
CREATE TYPE account_status  AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED');
CREATE TYPE saga_status      AS ENUM ('STARTED', 'LEDGER_HOLD_SUCCESS', 'VENDOR_CALL_SUCCESS', 'COMPLETED', 'FAILED', 'COMPENSATING');
CREATE TYPE service_type     AS ENUM ('FUND', 'TRANSFER', 'WITHDRAW', 'AIRTIME', 'DATA_SUBSCRIPTION', 'UTILITY_BILL', 'CABLE_TV');
CREATE TYPE funding_status   AS ENUM ('INITIATED', 'PENDING_PROVIDER', 'SUCCESS', 'FAILED');
CREATE TYPE service_status   AS ENUM ('INITIATED', 'PENDING_PROVIDER', 'SUCCESS', 'FAILED');

-- ============================================================================
-- 2. CLIENT IDENTITY BOUNDS & COMPLIANCE LAYER
-- ============================================================================
CREATE TABLE compliance_limits (
    tier compliance_tier PRIMARY KEY,
    single_transaction_limit BIGINT NOT NULL CHECK (single_transaction_limit > 0),
    daily_velocity_limit BIGINT NOT NULL CHECK (daily_velocity_limit > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO compliance_limits (tier, single_transaction_limit, daily_velocity_limit) VALUES 
('TIER_1', 10000, 50000),   
('TIER_2', 250000, 1000000), 
('TIER_3', 9223372036854775807, 9223372036854775807); 

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    --user_reference VARCHAR(64) UNIQUE NOT NULL, 
    --compliance_tier compliance_tier NOT NULL DEFAULT 'TIER_1',
    status account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    --password VARCHAR(64) NOT NULL,
    --firstname VARCHAR(64) NOT NULL,
    --lastname VARCHAR(64) NOT NULL,
    --phone_number VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 3. CORE LAYER
-- ============================================================================
CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ledger_account_id VARCHAR(64) UNIQUE NOT NULL, 
    available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0),
    held      BIGINT NOT NULL DEFAULT 0 CHECK (held >= 0),
    currency VARCHAR(3) NOT NULL, 
    --status account_status NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0, -- Optimistic locking
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    type service_type NOT NULL, -- FUND, P2P, VAS, PAYOUT
    amount BIGINT NOT NULL, -- Amounts are integer pence
    fee BIGINT NOT NULL DEFAULT 0,
    counterparty_wallet BIGINT,
    status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED
    --reference_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), 
  topic TEXT NOT NULL, 
  payload JSONB NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, 
  published_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE inbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), 
    event_id TEXT NOT NULL, 
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL, 
    code VARCHAR(255) UNIQUE NOT NULL, 
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL, 
    number VARCHAR(255) NOT NULL,
    bvn VARCHAR(255) UNIQUE NOT NULL,
    bank_id UUID NOT NULL REFERENCES banks(id),
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL,
    cost BIGINT NOT NULL,
    revenue BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name service_type NOT NULL, -- FIXED: Uses the enum type correctly
    service_providers_id UUID NOT NULL REFERENCES service_providers(id), -- FIXED: Added data type UUID and correctly ordered keywords
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);



/*
-- ============================================================================
-- 4. DISTRIBUTED IDEMPOTENCY LAYER
-- ============================================================================

-- Serves as an inner-database fallback safety grid to prevent duplicate user submissions
CREATE TABLE distributed_idempotency_registry (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_path VARCHAR(255) NOT NULL,
    hashed_payload BYTEA NOT NULL, -- SHA-256 checksum of incoming JSON variables
    cached_response JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Automatic index for cleaning out stale keys
CREATE INDEX idx_idempotency_expiry ON distributed_idempotency_registry (expires_at);

-- ============================================================================
-- 5. MULTI-VENDOR SAGA ORCHESTRATOR & VALUE-ADDED SERVICES (VAS)
-- ============================================================================

-- Tracks distributed transactional state lifecycles across external SaaS networks
CREATE TABLE saga_orchestration_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trace_id VARCHAR(64) NOT NULL, -- Unified W3C Trace Context (traceparent context propagation)
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(64) NOT NULL, -- 'VAS_AIRTIME', 'OUTBOUND_PAYOUT', 'FUNDING_INJECTION'
    amount BIGINT NOT NULL CHECK (amount > 0),
    status saga_status NOT NULL DEFAULT 'STARTED',
    
    -- State history blobs tracking individual downstream completions
    current_step INT NOT NULL DEFAULT 1,
    ledger_hold_reference VARCHAR(128),
    external_vendor_reference VARCHAR(128),
    error_message TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Value-Added Services (VAS) Metadata Store
CREATE TABLE vas_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    saga_id UUID NOT NULL REFERENCES saga_orchestration_logs(id) ON DELETE RESTRICT,
    type vas_type NOT NULL,
    provider_code VARCHAR(64) NOT NULL, -- e.g., 'MTN_NG'
    destination_identifier VARCHAR(255) NOT NULL, -- Encrypted or clear text routing endpoint
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 6. MULTI-CHANNEL WALLET FUNDING ENGINE
-- ============================================================================

CREATE TABLE wallet_funding_intents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    funding_reference VARCHAR(64) UNIQUE NOT NULL, -- e.g., 'fnd_01J8W3A1X9Y8W7V6'
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    status funding_status NOT NULL DEFAULT 'INITIATED',
    provider VARCHAR(64) NOT NULL, -- 'STRIPE', 'ADYEN', 'FLUTTERWAVE'
    provider_intent_id VARCHAR(255) UNIQUE, -- Stores external payload ids (e.g., Stripe pi_xyz)
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 7. NOTIFICATION TOPOLOGY STREAMS (THE OUTBOX PATTERN)
-- ============================================================================

CREATE TABLE ewallet_outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(64) NOT NULL, -- 'WALLET_CREATED', 'TRANSACTION_SETTLED', 'SAGA_COMPENSATED'
    aggregate_id VARCHAR(64) NOT NULL,
    event_payload JSONB NOT NULL, -- Structured JSON to pipe out to Kafka brokers
    is_dispatched BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ewallet_outbox_unprocessed 
ON ewallet_outbox_events (created_at ASC) WHERE is_dispatched = FALSE;

-- ============================================================================
-- 8. PROCEDURAL CONSTRAINTS, TRIGGERS, AND SECURITY FUNCTIONS
-- ============================================================================

/**
 * COMPLIANCE CONSTRAINT: Velocity Guardrail Trigger
 * Evaluates the incoming transaction size against the user's KYC tier cap.
 * Rejects operations immediately if the transaction breaches limit metrics.
 */
CREATE OR REPLACE FUNCTION enforce_kyc_velocity_bounds()
RETURNS TRIGGER AS $$
DECLARE
    v_tier compliance_tier;
    v_single_limit BIGINT;
    v_daily_limit BIGINT;
    v_current_daily_spend BIGINT;
    v_target_user_id UUID;
BEGIN
    -- Extract account metadata details
    SELECT user_id INTO v_target_user_id FROM wallets WHERE id = NEW.wallet_id;
    SELECT compliance_tier INTO v_tier FROM user_profiles WHERE id = v_target_user_id;
    
    -- Extract limits for the associated tier
    SELECT single_transaction_limit, daily_velocity_limit 
    INTO v_single_limit, v_daily_limit 
    FROM compliance_limits 
    WHERE tier = v_tier;

    -- Verification 1: Check single transaction bounds
    IF NEW.amount > v_single_limit THEN
        RAISE EXCEPTION 'Compliance Violation: Transaction amount % exceeds Single Limit of % for % profiles.', 
            NEW.amount, v_single_limit, v_tier;
    END IF;

    -- Verification 2: Calculate daily aggregated volume velocity
    SELECT COALESCE(SUM(amount), 0)
    INTO v_current_daily_spend
    FROM saga_orchestration_logs
    WHERE wallet_id = NEW.wallet_id
      AND status IN ('COMPLETED', 'STARTED', 'LEDGER_HOLD_SUCCESS')
      AND created_at >= CURRENT_DATE;

    IF (v_current_daily_spend + NEW.amount) > v_daily_limit THEN
        RAISE EXCEPTION 'Compliance Violation: Aggregated Daily Volume % exceeds Velocity Limit of % for % profiles.', 
            (v_current_daily_spend + NEW.amount), v_daily_limit, v_tier;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_kyc_bounds_before_saga
    BEFORE INSERT ON saga_orchestration_logs
    FOR EACH ROW
    EXECUTE FUNCTION enforce_kyc_velocity_bounds();
Use code with caution.💻 Production Implementation Showcase1. Inserting a PII Protected User Profile (AES-256 Encryption at Rest)This sample demonstrates how row-level security handles phone and email data masking using a cryptographic cipher key password:sqlINSERT INTO user_profiles (user_reference, compliance_tier, status, encrypted_phone, encrypted_email) 
VALUES (
    'usr_8821', 
    'TIER_1', 
    'ACTIVE',
    -- Encrypt with native AES-256 using a corporate encryption key string
    encrypt('07412345678'::bytea, 'pci_dss_encryption_key_passphrase', 'aes'),
    encrypt('greg@gregomebije.com'::bytea, 'pci_dss_encryption_key_passphrase', 'aes')
);
Use code with caution.2. Dynamic KYC Rule Enforcement Validation TestLet's verify that a Tier 1 customer cannot attempt a transaction that exceeds their daily limits (Tier 1 single_transaction_limit is capped at 10,000 minor units / $100.00):sql-- Fetch the target IDs generated by your system
SELECT id FROM user_profiles WHERE user_reference = 'usr_8821'; -- Returns a UUID
-- Assume Wallet UUID is generated and logged in wallets space...

-- Attempt to create a Saga Orchestration Log for $150.00 (15000 minor units)
INSERT INTO saga_orchestration_logs (trace_id, wallet_id, transaction_type, amount, status)
VALUES (
    '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01', -- W3C Trace Context Parent Token
    'wallet_uuid_here',
    'VAS_AIRTIME',
    15000, -- $150.00
    'STARTED'
);

-- RESULT ENGINE BOUNCE:
-- ERROR: Compliance Violation: Transaction amount 15000 exceeds Single Limit of 10000 for TIER_1 profiles.
-- CONTEXT: PL/pgSQL function enforce_kyc_velocity_bounds() line 17 at RAISE
*/