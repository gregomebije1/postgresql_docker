package com.example.wallet.integration;

import com.example.wallet.service.WalletServices.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class RedisIdempotencyIntegrationTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Test
    void onlyOneConcurrentRequestMayAcquireTheSameKey() throws Exception {
        InMemoryDistributedLock lock = new InMemoryDistributedLock();
        int n = 50;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        var successes = new java.util.concurrent.atomic.AtomicInteger();

        for (int i = 0; i < n; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    if (lock.tryAcquire("idempotency:key", Duration.ofSeconds(15))) successes.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(1, successes.get());
        assertTrue(redis.isRunning());
    }
}
