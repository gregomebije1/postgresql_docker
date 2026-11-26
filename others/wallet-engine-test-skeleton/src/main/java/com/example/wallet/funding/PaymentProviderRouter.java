package com.gregomebije.wallet.service.funding;

// Router interface
public interface PaymentProviderRouter {
    PaymentProvider resolve(String provider);
}