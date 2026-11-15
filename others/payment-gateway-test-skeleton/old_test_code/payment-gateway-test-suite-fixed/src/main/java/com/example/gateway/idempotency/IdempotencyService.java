package com.example.gateway.idempotency;

public interface IdempotencyService {
    boolean acquire(String key);
}
