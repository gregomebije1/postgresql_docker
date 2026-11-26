package com.gregomebije.wallet.service.funding;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.compliance.KycComplianceService;
import org.springframework.stereotype.Service;

@Service
public class FundingService {
    private final PaymentProviderRouter router;
    private final KycComplianceService compliance;

    public FundingService(PaymentProviderRouter router, KycComplianceService compliance) {
        this.router = router; 
        this.compliance = compliance;
    }

    public FundingIntentResponse createIntent(FundingIntentRequest request) {
        PaymentProvider provider = router.resolve(request.provider());
        PaymentIntentResult result = provider.createPaymentIntent(request);
        return new FundingIntentResponse("fnd-test", request.walletId(), request.amount(),
                request.currency(), FundingStatus.PENDING_PROVIDER,
                result.clientSecret(), result.redirectUrl());
    }
}
