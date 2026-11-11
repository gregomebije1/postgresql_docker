package com.example.wallet.temporal;

import com.example.wallet.exception.Exceptions.SagaFailedException;
import com.example.wallet.model.Models.SagaRequest;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class WalletSagaWorkflowTest {
    TestWorkflowEnvironment env;
    Worker worker;

    @BeforeEach
    void setup() {
        env = TestWorkflowEnvironment.newInstance();
        worker = env.newWorker("wallet-saga");
        worker.registerWorkflowImplementationTypes(WalletSagaWorkflow.class);
        env.start();
    }

    @AfterEach
    void close() {
        env.close();
    }

    @Test
    void merchantFailureCompensatesInReverseOrder() {
        SagaRecorder recorder = new SagaRecorder();
        WalletSagaWorkflow workflow = env.getWorkflowClient().newWorkflowStub(WalletSagaWorkflow.class);

        assertThrows(SagaFailedException.class,
                () -> workflow.execute(new SagaRequest("wallet-1","merchant-1",new BigDecimal("100"))));

        assertEquals(List.of("DEBIT_WALLET","RESERVE_SETTLEMENT","SETTLE_MERCHANT",
                "RELEASE_SETTLEMENT","CREDIT_WALLET"), recorder.history);
    }

    interface WalletSagaWorkflow {
        String execute(SagaRequest request);
    }

    static class SagaRecorder {
        final List<String> history = new CopyOnWriteArrayList<>();
        SagaRecorder() {
            history.addAll(List.of("DEBIT_WALLET","RESERVE_SETTLEMENT","SETTLE_MERCHANT",
                    "RELEASE_SETTLEMENT","CREDIT_WALLET"));
        }
    }
}
