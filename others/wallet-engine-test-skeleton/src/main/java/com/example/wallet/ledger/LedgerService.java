package com.gregomebije.wallet.service.ledger;

import java.math.BigDecimal;

public interface LedgerService {
    String placeHold(String walletId, BigDecimal amount);
    void captureHold(String holdId);
    void releaseHold(String holdId);
}