package com.gregomebije.wallet.service.idempotency;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryDistributedLock implements DistributedLockService {
    private final Set<String> keys = ConcurrentHashMap.newKeySet();

    @Override 
    public boolean tryAcquire(String key, Duration ttl) { 
        return keys.add(key); 
    }
}
