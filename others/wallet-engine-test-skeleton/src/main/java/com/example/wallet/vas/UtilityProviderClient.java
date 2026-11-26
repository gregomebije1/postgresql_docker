package com.gregomebije.wallet.service.vas;

import com.gregomebije.wallet.model.Models.*;

public interface UtilityProviderClient {
        UtilityPurchaseResult purchase(VasPurchaseRequest request);
    }
