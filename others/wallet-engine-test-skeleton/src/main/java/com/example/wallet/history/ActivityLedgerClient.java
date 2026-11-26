package com.gregomebije.wallet.service.history;

import java.util.*;


import com.gregomebije.wallet.model.Models.LedgerTransaction;

public interface ActivityLedgerClient {
    List<LedgerTransaction> getTransactions(String walletId);
}