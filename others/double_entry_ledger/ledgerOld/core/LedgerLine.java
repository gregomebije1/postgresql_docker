package com.example.ledger.core;

public record LedgerLine(
        String accountId,
        LedgerLineType type,
        long amount
) {}
