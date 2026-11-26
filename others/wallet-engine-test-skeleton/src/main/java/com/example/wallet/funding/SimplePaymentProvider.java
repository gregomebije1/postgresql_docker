package com.gregomebije.wallet.service.funding;

import com.gregomebije.wallet.model.Models.FundingIntentRequest;
import com.gregomebije.wallet.model.Models.PaymentIntentResult;

import org.springframework.stereotype.Service;

@Service
public class SimplePaymentProvider implements PaymentProvider {

    @Override
    public PaymentIntentResult createPaymentIntent(FundingIntentRequest request) {
        return new PaymentIntentResult("reference", "clientSecret", "redirectUrl");
    }
}
