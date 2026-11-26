package com.gregomebije.wallet.service.transfer;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.ledger.LedgerClient;
import com.gregomebije.wallet.exception.Exceptions.ProviderTimeoutException;
import com.gregomebije.wallet.exception.Exceptions.TransferPendingRetryException;
import com.gregomebije.wallet.service.notification.RetryMessage;
import com.gregomebije.wallet.service.notification.RetryQueue;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class TransferService {
    private final LedgerClient ledger;
    private final RetryQueue retryQueue;
    public TransferService(LedgerClient ledger, RetryQueue retryQueue) {
        this.ledger = ledger; this.retryQueue = retryQueue;
    }
    public LedgerTransactionResult transfer(P2PRequest request, String key) {
        try { return ledger.createP2PTransaction(request); }
        catch (ProviderTimeoutException e) {
            retryQueue.publish(new RetryMessage("tx-123"));
            throw new TransferPendingRetryException();
        }
    }
}