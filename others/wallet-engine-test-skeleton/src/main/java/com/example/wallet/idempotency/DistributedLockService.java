package com.gregomebije.wallet.service.idempotency;

import java.time.Duration;

public interface DistributedLockService {
    boolean tryAcquire(String key, Duration ttl);
}
