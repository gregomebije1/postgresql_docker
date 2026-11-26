package com.gregomebije.wallet.service.vas;

import com.gregomebije.wallet.service.ledger.*;
import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.exception.Exceptions.VasPurchaseFailedException;

import org.springframework.stereotype.Service;

@Service
public class VasPurchaseService {
    private final LedgerService ledger;
    private final UtilityProviderClient provider;

    public VasPurchaseService(LedgerService ledger, UtilityProviderClient provider) {
        this.ledger = ledger; this.provider = provider;
    }

    public void purchase(VasPurchaseRequest request) {
        String hold = ledger.placeHold(request.walletId(), request.amount());
        try {
            UtilityPurchaseResult result = provider.purchase(request);
            if (!result.success()) throw new VasPurchaseFailedException();
            ledger.captureHold(hold);
        } catch (RuntimeException e) {
            ledger.releaseHold(hold);
            throw e instanceof VasPurchaseFailedException ? e : new VasPurchaseFailedException();
        }
    }
}