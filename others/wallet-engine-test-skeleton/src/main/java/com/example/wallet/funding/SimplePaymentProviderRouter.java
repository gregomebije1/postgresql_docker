package com.gregomebije.wallet.service.funding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.gregomebije.wallet.service.notification.RetryMessage;

import org.springframework.stereotype.Service;

@Service
public class SimplePaymentProviderRouter implements PaymentProviderRouter {
    private final Map<String, PaymentProvider> providers = new ConcurrentHashMap<>();

    @Override
    public PaymentProvider resolve(String provider) {
        return providers.get(provider);
    }
}
