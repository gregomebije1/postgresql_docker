package com.gregomebije.wallet.service.ledger;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class WalletDebitService {
    private final TestLedgerRepository ledger;
    public WalletDebitService(TestLedgerRepository ledger) { this.ledger = ledger; }
    public void debit(String accountId, BigDecimal amount) { ledger.debit(accountId, amount); }
}