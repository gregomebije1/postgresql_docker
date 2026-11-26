package com.gregomebije.wallet.service.notification;

import com.gregomebije.wallet.model.Models.*;

public interface NotificationPublisher {
    void publish(LedgerTransactionResult result);
} 
