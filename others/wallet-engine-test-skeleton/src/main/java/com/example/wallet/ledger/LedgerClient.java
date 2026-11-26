package com.gregomebije.wallet.service.ledger;

import com.gregomebije.wallet.model.Models.*;

public interface LedgerClient {
        LedgerTransactionResult createP2PTransaction(P2PRequest request);
    }