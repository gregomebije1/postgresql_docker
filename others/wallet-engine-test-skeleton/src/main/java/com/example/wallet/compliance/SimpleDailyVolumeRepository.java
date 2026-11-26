package com.gregomebije.wallet.service.compliance;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gregomebije.wallet.service.notification.RetryMessage;

@Service
public class SimpleDailyVolumeRepository implements DailyVolumeRepository {
    private final Map<String, BigDecimal> volumes = new ConcurrentHashMap<>();
    
    public void set(String userId, BigDecimal amount) { 
        volumes.put(userId, amount); 
    }
    
    @Override 
    public BigDecimal getDailySuccessfulVolume(String userId) {
        return volumes.getOrDefault(userId, BigDecimal.ZERO);
    }
}
