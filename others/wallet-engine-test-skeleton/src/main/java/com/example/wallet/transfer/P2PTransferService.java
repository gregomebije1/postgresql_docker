package com.gregomebije.wallet.service.transfer;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.compliance.KycComplianceService;
import com.gregomebije.wallet.service.idempotency.IdempotencyService;
import com.gregomebije.wallet.service.ledger.LedgerClient;
import com.gregomebije.wallet.service.notification.NotificationPublisher;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class P2PTransferService {
    private final IdempotencyService idempotency;
    private final KycComplianceService compliance;
    private final LedgerClient ledger;
    private final NotificationPublisher publisher;

    public P2PTransferService(IdempotencyService idempotency, KycComplianceService compliance,
                              LedgerClient ledger, NotificationPublisher publisher) {
        this.idempotency = idempotency; 
        this.compliance = compliance;
        this.ledger = ledger; 
        this.publisher = publisher;
    }

    public P2PResponse transfer(P2PRequest request, String key) {
        idempotency.acquire(key);
        compliance.validate(request.senderWalletId(), ComplianceTier.TIER_2, request.amount());
        LedgerTransactionResult tx = ledger.createP2PTransaction(request);
        publisher.publish(tx);
        return new P2PResponse(tx.transactionId(), request.senderWalletId(),
                request.receiverWalletId(), request.amount(), BigDecimal.ZERO);
    }
}
