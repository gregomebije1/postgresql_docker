package com.gregomebije.wallet.service.funding;

import com.gregomebije.wallet.model.Models.FundingIntentRequest;
import com.gregomebije.wallet.model.Models.PaymentIntentResult;

public interface PaymentProvider {
    PaymentIntentResult createPaymentIntent(FundingIntentRequest request);
}