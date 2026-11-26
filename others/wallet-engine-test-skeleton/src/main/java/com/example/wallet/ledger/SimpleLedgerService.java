package com.gregomebije.wallet.service.ledger;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service 
public class SimpleLedgerService implements LedgerService {
    @Override 
    public String placeHold(String walletId, BigDecimal amount) {
        return "";
    }

    @Override
    public void captureHold(String holdId) {
        //empty implementation
    }

    @Override
    public void releaseHold(String holdId) {
        //empty implementation 
    }
}