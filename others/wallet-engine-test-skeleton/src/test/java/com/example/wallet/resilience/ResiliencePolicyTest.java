package com.example.wallet.resilience;

import io.github.resilience4j.circuitbreaker.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResiliencePolicyTest {
    @Test
    void failureThresholdShouldBe50Percent() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .build();

        assertEquals(50.0f, config.getFailureRateThreshold());
    }

    @Test
    void openCircuitShouldFailFast() {
        CircuitBreaker cb = CircuitBreaker.of("ledger",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .minimumNumberOfCalls(2)
                        .build());

        cb.transitionToOpenState();

        assertThrows(CallNotPermittedException.class,
                () -> cb.executeRunnable(() -> {}));
    }
}
