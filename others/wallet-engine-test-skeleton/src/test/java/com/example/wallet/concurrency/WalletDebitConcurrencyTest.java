package com.example.wallet.concurrency;

import com.example.wallet.exception.Exceptions.InsufficientFundsException;
import com.example.wallet.service.WalletServices.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class WalletDebitConcurrencyTest {
    private TestLedgerRepository ledger;
    private WalletDebitService service;

    @BeforeEach
    void setup() {
        ledger = new TestLedgerRepository();
        ledger.createAccount("acc", new BigDecimal("100.00"));
        service = new WalletDebitService(ledger);
    }

    @Test
    void exactlyTenOfFiftyConcurrentDebitsShouldSucceed() throws Exception {
        int n = 50;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        for (int i = 0; i < n; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    service.debit("acc", new BigDecimal("10.00"));
                    success.incrementAndGet();
                } catch (InsufficientFundsException e) {
                    insufficient.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.set(t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertNull(unexpected.get());
        assertEquals(10, success.get());
        assertEquals(40, insufficient.get());
        assertEquals(new BigDecimal("0.00"), ledger.getBalance("acc"));
        assertEquals(10, ledger.countSuccessfulDebits("acc"));
        assertTrue(ledger.getBalance("acc").compareTo(BigDecimal.ZERO) >= 0);
    }
}
