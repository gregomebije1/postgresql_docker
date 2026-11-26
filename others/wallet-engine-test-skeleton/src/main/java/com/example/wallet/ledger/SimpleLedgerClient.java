package com.gregomebije.wallet.service.ledger;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;


import com.gregomebije.wallet.model.Models.*;

@Service
public class SimpleLedgerClient implements LedgerClient {
  
    @Override
    public LedgerTransactionResult createP2PTransaction(P2PRequest request) {
        return new LedgerTransactionResult("id");
    }

}
