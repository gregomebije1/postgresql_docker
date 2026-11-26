package com.gregomebije.wallet.service.history;

import java.util.*;

import org.springframework.stereotype.Service;  

import com.gregomebije.wallet.model.Models.LedgerTransaction;

@Service
public class SimpleActivityLedgerClient implements ActivityLedgerClient {
    List<LedgerTransaction> ledgerTransactions = new ArrayList<>();

    public List<LedgerTransaction> getTransactions(String walletId) {
        return this.ledgerTransactions;
    }
}