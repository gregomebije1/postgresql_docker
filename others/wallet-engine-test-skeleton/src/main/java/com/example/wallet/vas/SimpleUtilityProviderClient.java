package com.gregomebije.wallet.service.vas;

import org.springframework.stereotype.Service;
import com.gregomebije.wallet.model.Models.*;

@Service
public class SimpleUtilityProviderClient implements UtilityProviderClient {
    @Override
    public UtilityPurchaseResult purchase(VasPurchaseRequest request) {
        return new UtilityPurchaseResult(true, "");
  
    }
}
