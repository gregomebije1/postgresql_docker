CREATE TABLE ledger_accounts (
    tenant_id   UUID NOT NULL,
    id          UUID NOT NULL,
    currency    CHAR(3) NOT NULL,
    account_type TEXT NOT NULL CHECK (
        account_type IN (
            'ASSET', 'LIABILITY', 'EQUITY',
            'REVENUE', 'EXPENSE'
        )
    ),
    status      TEXT NOT NULL CHECK (
        status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')
    ),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (tenant_id, id),
    UNIQUE (tenant_id, id, currency)
);

CREATE TABLE ledger_transactions (
    tenant_id       UUID NOT NULL,
    id              UUID NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash    BYTEA NOT NULL,
    narration       TEXT NOT NULL,
    status          TEXT NOT NULL CHECK (
        status IN ('POSTED', 'REVERSED')
    ),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (tenant_id, id),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE ledger_entries (
    tenant_id      UUID NOT NULL,
    transaction_id UUID NOT NULL,
    line_number    INTEGER NOT NULL,
    account_id     UUID NOT NULL,
    currency       CHAR(3) NOT NULL,
    direction      TEXT NOT NULL CHECK (
        direction IN ('DEBIT', 'CREDIT')
    ),
    amount_minor   BIGINT NOT NULL CHECK (amount_minor > 0),

    PRIMARY KEY (tenant_id, transaction_id, line_number),

    FOREIGN KEY (tenant_id, transaction_id)
        REFERENCES ledger_transactions (tenant_id, id),

    FOREIGN KEY (tenant_id, account_id, currency)
        REFERENCES ledger_accounts (tenant_id, id, currency)
);

CREATE INDEX ledger_entries_account_history_idx
    ON ledger_entries
       (tenant_id, account_id, transaction_id);