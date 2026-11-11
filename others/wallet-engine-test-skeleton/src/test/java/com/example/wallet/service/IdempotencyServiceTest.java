package com.gregomebije.wallet.service;

import com.gregomebije.wallet.exception.Exceptions.DuplicateRequestException;
import com.gregomebije.wallet.service.idempotency.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {
    @Test
    void firstRequestAcquires15SecondLock() {
        DistributedLockService lock = mock(DistributedLockService.class);
        when(lock.tryAcquire("idempotency:key", Duration.ofSeconds(15))).thenReturn(true);

        assertDoesNotThrow(() -> new IdempotencyService(lock).acquire("key"));
        verify(lock).tryAcquire("idempotency:key", Duration.ofSeconds(15));
    }

    @Test
    void duplicateReturnsConflict() {
        DistributedLockService lock = mock(DistributedLockService.class);
        when(lock.tryAcquire(anyString(), any())).thenReturn(false);

        assertThrows(DuplicateRequestException.class,
                () -> new IdempotencyService(lock).acquire("duplicate"));
    }
}
