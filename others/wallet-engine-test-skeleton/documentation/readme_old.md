
This e-wallet engine that can allow users to perform common everyday transactions including fund transfers, cash withdrawals, 
airtime purchases, data subscriptions and utility or cable TV bill payments.
This is a customer-facing orchestrator layer. This engine processes client applications, validates user identity bounds, 
enforces compliance tiers, and manages distributed transactions across external vendor networks. 
It maintains no local balance database, instead leasing decoupled API interfaces from a distributed double-entry ledger engine and a Settlement Engine.

## Core System Requirements

### 1. Functional Requirements
* **Dynamic KYC Tier Compliance:** Evaluates incoming user actions against velocity and value volume boundaries based on identity tier mappings (e.g., Tier 1 cap at \$100/day while verified Tier 2 profiles enjoy uncapped standard operational levels).
* **Multi-Vendor Saga Orchestrator:** Manages multi-stage transactional state lifecycles across external SaaS APIs, executing automated compensating rollbacks if a vendor service fails.
* **Consolidated Activity Mapping:** Reconstructs user histories by pulling raw ledger records from a distributed double-entry ledger engine and pairing them with local user context metadata.
* **Notification Topology Streams:** Emits structured events directly into an Apache Kafka message bus upon ledger transaction affirmation to dispatch real-time SMS, email, and Webhook web-push receipts.

### 2. Non-Functional Distributed Requirements
* **Saga Pattern Resilience & Dead-Letter Storage:** Long-running cross-network workflows must persist state via an event sourcing backbone (e.g., Apache Kafka). Unresolvable multi-vendor transaction state drops must automatically isolate into dedicated Dead-Letter Queues (DLQ) for human audit.
* **Distributed Idempotency Layer:** Implements a distributed cache locking pattern (Redis cluster utilizing a Redlock algorithm) to intercept incoming user actions. Duplicate requests with identical keys are rejected with a `409 Conflict` within a 15-second window.
* **Partial Failures & Circuit Breaking:** Outbound API proxies 
   to external distributed double-entry engine and payment providers  
   must implement automated circuit breakers (e.g., Resilience4j) configured with a 50% failure rate trip-wire. If a vendor times out, requests must gracefully fail-fast or route to offline retry queues to preserve user experience.
* **Observability Context Propagation:** Every user-initiated request must generate a unified W3C Trace Context (`traceparent`). This token must be forced into the headers of all downstream API calls to Ledger engine and payment-gateway to enable end-to-end distributed tracking across corporate boundaries.
*   **Data Masking & PII Protection:** Enforces row-level AES-256 encryption at rest for customer metadata records, maintaining compliance alignment across PCI-DSS boundaries.


## Architecture 
### 1. Orchestrated Saga Pattern
Distributed consistency is maintained asynchronously across microservices using a centralized orchestrator rather than distributed locking.
*   **Coordination:** A central coordinator manages workflow states between the **Wallet Service**, **Ledger Service**, and **Merchant Settlement Service**.
*   **Compensation:** If a downstream service fails, the orchestrator triggers reverse operations sequentially to undo completed steps.


## Testing Strategy

### 1. Saga Failure Mode Simulation
*   Uses Temporal's native testing framework (`TestWorkflowEnvironment`) to inject synthetic network delays or explicitly throw a `RuntimeFailure` within the *Merchant Settlement Service*.
*   **Verification:** Asserts that all reverse compensating steps trigger in strict reverse order, leaving system funds in their exact pre-transaction state.

### 2. High-Concurrency Race Condition Testing
*   **Execution:** Leverages Java's `CountDownLatch` and `ExecutorService` to fire **50 parallel threads** attempting to deduct `$10.00` simultaneously from a single account initialized with a `$100.00` balance.
*   **Success Criteria:** 
    *   Exactly **10 threads** complete successfully (reducing the absolute balance to `$0.00`).
    *   **40 threads** fail safely, throwing either an `InsufficientFundsException` or an optimistic locking collision exception.
    *   No data drift or negative balances occur.


## Essential Core API Endpoints

### 1. Provision New Wallet Account Space
* **Endpoint:** `POST /api/v1/wallets/accounts`
* **Request Payload:**
```json
{
  "user_id": "usr_8821",
  "compliance_tier": "TIER_1",
  "currency": "USD"
}
```
* **Response (201 Created):**
```json
{
  "status": "success",
  "data": {
    "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
    "ledger_account_id": "acc_01J8W2Z1X9Y8W7V6U5T4S3R2Q1",
    "user_id": "usr_9876543210",
    "currency": "USD",
    "status": "ACTIVE",
    "created_at": "2026-09-19T12:55:00Z"
  }
}
```


### 2. Multi-Channel Wallet Funding Engine
#### `POST /api/v1/wallets/funding/intent`
Initiates a payment injection lifecycle via external providers (e.g., Payment gateway engine, Stripe, Adyen, Flutterwave).

* **Request Payload:**
```json
{
  "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
  "amount": 5000,
  "currency": "USD",
  "payment_method": "CREDIT_CARD",
  "provider": "STRIPE"
}
```
* **Response (200 OK):**
```json
{
  "status": "success",
  "data": {
    "funding_id": "fnd_01J8W3A1X9Y8W7V6",
    "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
    "amount": 5000,
    "currency": "USD",
    "status": "PENDING_PROVIDER",
    "client_secret": "pi_3MtwbL_secret_xyz",
    "redirect_url": "https://checkout.stripe.com/pay/c_xyz"
  }
}
```
#### `POST /api/v1/wallets/funding/webhook/{provider}`
Asynchronous ingress endpoint receiving validated notifications from payment providers. Upon successful capture, this endpoint calls `POST /api/v1/ledger/transactions` on the Ledger Service to move funds from the `SYSTEM_DEPOSIT_CLEARING` account to the user's ledger account.

* **Headers:** `X-Provider-Signature: t=1672531199,v1=g3b2...`
* **Response (200 OK):**
```json
{
  "status": "acknowledged",
  "ledger_transaction_id": "tx_01J8W3B5M9P7Q4R1"
}
```

### 3. Initiate External Transfer Payout Request
* **Endpoint:** `POST /api/v1/transfers/outbound`
* **Headers:** `X-Idempotency-Key: c9b22e11-128a-4402-b883-29a38f32dd71`
* **Payload:**
```json
{
  "sender_id": "usr_8821",
  "amount": 5000,
  "currency": "USD",
  "bank_routing_code": "058",
  "destination_account": "0123456789"
}
```


### 4. Peer-to-Peer (P2P) Transfers
#### `POST /api/v1/transactions/p2p`
Orchestrates an instant domestic internal transfer by submitting a multi-line transaction payload directly to the distributed ledger core.

* **Headers:** `X-Idempotency-Key: 8e5f2b84-482a-4f51-b0e6-0563b7df0ad1`
* **Request Payload:**
```json
{
  "sender_wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
  "receiver_wallet_id": "wlt_01J8W5K3M2N1P0Q9",
  "amount": 2500,
  "currency": "USD",
  "narration": "Dinner split"
}
```
* **Response (202 Accepted):**
```json
{
  "status": "success",
  "transaction_id": "tx_01J8W3C8K9N7M4P1",
  "summary": {
    "sender_wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
    "receiver_wallet_id": "wlt_01J8W5K3M2N1P0Q9",
    "amount": 2500,
    "fee_charged": 0
  }
}
```



### 5. Value-Added Services (VAS) & Bill Payments
#### `POST /api/v1/vas/purchase`
Executes digital utility purchases. This service operates via a Two-Phase Commit architecture:
1. Calls the Ledger Service to put a hold or freeze on the user's available balance.
2. Dispatches a call to the third-party utility network (e.g., Telecom API).
3. If successful, captures the hold in the ledger. If failed, releases the hold automatically.

* **Request Payload:**
```json
{
  "wallet_id": "wlt_01J8W2Z1X9Y8W7V6",
  "vas_type": "AIRTIME",
  "provider": "MTN_NG",
  "amount": 1000,
  "destination_identifier": "+2348030000001"
}
```

# Useful commands
./mvnw clean compile
./mvnw spring-boot:run
./mvnw test
mvn clean test-compile

=====
🔎 Architectural Analysis & Data MappingAccording to the design blueprint, this system acts as a customer-facing orchestrator layer. Because it leases decoupled account interfaces from a external Ledger Engine, this database focuses on tracking user context metadata, compliance velocity boundaries, cryptographic verification parameters, and long-running Saga state transitions.This production-grade PostgreSQL DDL schema incorporates row-level security primitives, AES-256 data masking for PII protection, and strict transactional state constraints.Verification: Distributed Alignment MatrixAPI EndpointDatabase Storage TargetCore Active Safety Measures EnforcedPOST /wallets/accountsuser_profiles, walletsRow-Level AES-256 PII protection prevents plain-text identity leaks.POST /wallets/funding/intentwallet_funding_intentsStrict unique constraints on provider_intent_id to block dual-webhook injections.POST /transfers/outbounddistributed_idempotency_registryIntercepts keys to guarantee safe execution across third-party networks.POST /vas/purchasesaga_orchestration_logs, ewallet_outbox_eventsUses the Transactional Outbox pattern to guarantee that events reach Kafka for processing.

=====
package com.gregomebije.gateway.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletConcurrencyRaceConditionTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private PaymentService paymentService; // The Orchestrator Layer service under test

    @Test
    @DisplayName("High-Concurrency Race Condition: 50 Threads Deducting $10 from $100 Balance")
    void verifyHighConcurrencyRaceConditionSafety() throws InterruptedException {
        // --- 1. ARRANGE & SETUP ---
        final int totalThreads = 50;
        final int expectedSuccesses = 10;
        final int expectedFailures = 40;
        
        final BigDecimal initialBalance = new BigDecimal("100.00");
        final BigDecimal deductionAmount = new BigDecimal("10.00");

        // Atomic counters to safely track thread results across asynchronous contexts
        AtomicInteger successfulTransactions = new AtomicInteger(0);
        AtomicInteger failedTransactions = new AtomicInteger(0);

        // Core concurrency synchronization latches
        CountDownLatch startLineLatch = new CountDownLatch(1);      // Force all 50 threads to fire simultaneously
        CountDownLatch finishLineLatch = new CountDownLatch(totalThreads); // Wait for all 50 threads to finish execution

        // Thread pool manager to execute parallel requests
        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);

        // Mock Behavior Setup:
        // Simulate a real ledger lease database state. The first 10 calls succeed, subsequent calls throw
        // an InsufficientFundsException or an OptimisticLockingFailureException.
        when(idempotencyService.acquire(anyString())).thenReturn(true);
        
        // Use Mockito's thenAnswer to track state mutation across threads dynamically
        final AtomicInteger remainingBalanceUnits = new AtomicInteger(100); // Track cents/units ($100)
        
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            // Atomically decrement by 10 units ($10)
            int previousBalance = remainingBalanceUnits.get();
            while (previousBalance >= 10) {
                if (remainingBalanceUnits.compareAndSet(previousBalance, previousBalance - 10)) {
                    return invocation.getArgument(0); // Success path
                }
                previousBalance = remainingBalanceUnits.get(); // Retry CAS on optimistic collision
            }
            throw new InsufficientFundsException("Insufficient funds in wallet cache or ledger lease pool.");
        });

        // --- 2. ACT (SIMULATE SIMULTANEOUS CONCURRENT SPIKE) ---
        for (int i = 0; i < totalThreads; i++) {
            final String uniqueIdempotencyKey = "idem-key-" + i;
            final String uniquePaymentReference = "pay-ref-" + i;

            executorService.submit(() -> {
                try {
                    // All threads halt here until the master latch opens
                    startLineLatch.await(); 

                    PaymentRequest request = new PaymentRequest(
                            uniquePaymentReference, 
                            deductionAmount, 
                            "USD"
                    );

                    // Execute the service orchestration layer call
                    paymentService.create(uniqueIdempotencyKey, request);
                    successfulTransactions.incrementAndGet();

                } catch (InsufficientFundsException | OptimisticLockingException e) {
                    failedTransactions.incrementAndGet();
                } catch (Exception e) {
                    // Catch unexpected runtime thread exceptions to prevent test deadlock hangs
                    failedTransactions.incrementAndGet();
                } finally {
                    finishLineLatch.countDown();
                }
            });
        }

        // Drop the gate: All 50 threads race into the payment logic simultaneously
        startLineLatch.countDown();

        // Await thread completion with a strict fail-safe execution timeout barrier
        boolean threadsFinishedCleanly = finishLineLatch.await(5, TimeUnit.SECONDS);

        // Clean up allocation pools immediately
        executorService.shutdown();

        // --- 3. ASSERT (SUCCESS CRITERIA VERIFICATION) ---
        assertThat(threadsFinishedCleanly)
                .withFailMessage("Test timed out before all parallel execution lanes completed.")
                .isTrue();

        // Verify that exactly 10 threads completed successfully ($100 / $10 = 10)
        assertThat(successfulTransactions.get())
                .withFailMessage("Expected exactly %d transactions to pass but got %d", expectedSuccesses, successfulTransactions.get())
                .isEqualTo(expectedSuccesses);

        // Verify that exactly 40 threads failed safely without dropping into negative state balances
        assertThat(failedTransactions.get())
                .withFailMessage("Expected exactly %d transactions to fail but got %d", expectedFailures, failedTransactions.get())
                .isEqualTo(expectedFailures);

        // Confirm zero data drift: remaining value units must be exactly 0
        assertThat(remainingBalanceUnits.get())
                .withFailMessage("Data drift detected! Balance dropped illegally to: " + remainingBalanceUnits.get())
                .isZero();
    }
}
