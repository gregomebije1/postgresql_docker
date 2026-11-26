package com.gregomebije.wallet.service.ledger;

import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.gregomebije.wallet.exception.Exceptions.InsufficientFundsException;

@Service
public class TestLedgerRepository {
    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> successes = new ConcurrentHashMap<>();

    public void reset() { balances.clear(); successes.clear(); }
    public void createAccount(String id, BigDecimal balance) {
        balances.put(id, balance); successes.put(id, new AtomicInteger());
    }
    public BigDecimal getBalance(String id) { return balances.get(id); }
    public int countSuccessfulDebits(String id) { return successes.get(id).get(); }
    public int countNegativeBalanceSnapshots(String id) { return 0; }

    public synchronized void debit(String id, BigDecimal amount) {
        BigDecimal current = balances.get(id);
        if (current.compareTo(amount) < 0) throw new InsufficientFundsException();
        balances.put(id, current.subtract(amount));
        successes.get(id).incrementAndGet();
    }
}