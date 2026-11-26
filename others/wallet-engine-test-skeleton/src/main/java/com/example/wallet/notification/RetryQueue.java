package com.gregomebije.wallet.service.notification;

public interface RetryQueue { void publish(RetryMessage message); }