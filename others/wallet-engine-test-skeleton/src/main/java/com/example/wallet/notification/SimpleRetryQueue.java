package com.gregomebije.wallet.service.notification;

import org.springframework.stereotype.Service;

@Service
public class SimpleRetryQueue implements RetryQueue { 

    @Override
    public void publish(RetryMessage message) {
        //TODO:
    }
}