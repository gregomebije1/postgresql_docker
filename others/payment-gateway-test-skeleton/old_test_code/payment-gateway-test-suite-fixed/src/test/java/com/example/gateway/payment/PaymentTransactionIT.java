package com.example.gateway.payment;

import com.example.gateway.idempotency.IdempotencyService;
import com.example.gateway.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentTransactionIT {

    @Autowired PaymentService service;
    @Autowired PaymentRepository payments;
    @Autowired OutboxRepository outbox;
    @MockitoBean IdempotencyService idempotency;

    @Test
    void paymentAndOutboxRollbackTogether() {
        when(idempotency.acquire("rollback-idem")).thenReturn(true);

        assertThatThrownBy(() -> service.createAndFail(
                "rollback-idem",
                new PaymentRequest("rollback-pay", new BigDecimal("10.00"), "GBP")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(payments.findByReference("rollback-pay")).isEmpty();
        assertThat(outbox.count()).isZero();
    }
}
