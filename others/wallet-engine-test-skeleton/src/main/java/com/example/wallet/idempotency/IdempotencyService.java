package com.gregomebije.wallet.service.idempotency;

import com.gregomebije.wallet.exception.Exceptions.DuplicateRequestException;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {
    private final DistributedLockService lock;

    public IdempotencyService(DistributedLockService lock) { 
        this.lock = lock; 
    }

    public void acquire(String key) {
        if (!lock.tryAcquire("idempotency:" + key, Duration.ofSeconds(15))) {
            throw new DuplicateRequestException();
        }
    }
}
