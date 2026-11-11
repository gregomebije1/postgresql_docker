package com.example.gateway.payment;

import com.example.gateway.idempotency.IdempotencyService;
import com.example.gateway.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceUnitTest {

    @Test
    void createsPaymentAndOutboxEvent() {
        PaymentRepository payments = mock(PaymentRepository.class);
        OutboxRepository outbox = mock(OutboxRepository.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);

        when(idempotency.acquire("idem-1")).thenReturn(true);
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentService service = new PaymentService(payments, outbox, idempotency);

        PaymentResponse response = service.create(
                "idem-1",
                new PaymentRequest("pay-1", new BigDecimal("100.00"), "GBP"));

        assertThat(response.reference()).isEqualTo("pay-1");
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        verify(outbox).save(any());
    }

    @Test
    void duplicateDoesNotCreateAnotherPayment() {
        PaymentRepository payments = mock(PaymentRepository.class);
        OutboxRepository outbox = mock(OutboxRepository.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);

        when(idempotency.acquire("idem-1")).thenReturn(false);
        Payment existing = new Payment("pay-1", new BigDecimal("10.00"), "GBP");
        when(payments.findByReference("pay-1")).thenReturn(java.util.Optional.of(existing));

        PaymentService service = new PaymentService(payments, outbox, idempotency);
        service.create("idem-1",
                new PaymentRequest("pay-1", new BigDecimal("10.00"), "GBP"));

        verify(payments, never()).save(any());
        verify(outbox, never()).save(any());
    }
}
