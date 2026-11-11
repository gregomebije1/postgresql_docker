package com.example.ledger.core;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LedgerValidator {

    public void validate(List<LedgerLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Ledger must contain at least one line");
        }

        long debits = 0;
        long credits = 0;

        for (LedgerLine line : lines) {
            if (line == null || line.accountId() == null || line.accountId().isBlank()) {
                throw new IllegalArgumentException("Account ID is required");
            }
            if (line.amount() <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }

            if (line.type() == LedgerLineType.DEBIT) {
                debits = Math.addExact(debits, line.amount());
            } else if (line.type() == LedgerLineType.CREDIT) {
                credits = Math.addExact(credits, line.amount());
            } else {
                throw new IllegalArgumentException("Unsupported ledger type");
            }
        }

        if (debits != credits) {
            throw new UnbalancedLedgerException(
                    "Ledger is unbalanced: variance=" + (debits - credits)
            );
        }
    }
}
