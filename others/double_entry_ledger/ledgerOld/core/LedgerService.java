package com.gregomebije.ledger.core;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerValidator validator;

    public LedgerService(LedgerValidator validator) {
        this.validator = validator;
    }

    @Transactional
    public String postTransaction(String tenantId, PostTransactionRequest request) {
        validator.validate(request.lines());
        //TODO: Production implementation should persist immutable journal rows here.
        return UUID.randomUUID().toString();
    }

    @Transactional
    public String postTransactionAndFail(String tenantId, PostTransactionRequest request) {
        validator.validate(request.lines());
        throw new RuntimeException("Forced rollback for test");
    }
}
