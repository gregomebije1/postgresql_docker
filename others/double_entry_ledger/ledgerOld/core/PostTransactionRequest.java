package com.example.ledger.core;

import java.util.List;

public record PostTransactionRequest(
        String narration,
        List<LedgerLine> lines
) {}
