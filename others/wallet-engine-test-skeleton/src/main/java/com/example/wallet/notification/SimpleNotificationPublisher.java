package com.gregomebije.wallet.service.notification;

import com.gregomebije.wallet.model.Models.*;
import org.springframework.stereotype.Service;

@Service
public class SimpleNotificationPublisher implements NotificationPublisher {

    @Override
    public void publish(LedgerTransactionResult result) {
        //TODO: implement
    }
} 
