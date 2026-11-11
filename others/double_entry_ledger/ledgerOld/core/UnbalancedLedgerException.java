package com.example.ledger.core;

public class UnbalancedLedgerException extends RuntimeException {
    public UnbalancedLedgerException(String message) {
        super(message);
    }
}
