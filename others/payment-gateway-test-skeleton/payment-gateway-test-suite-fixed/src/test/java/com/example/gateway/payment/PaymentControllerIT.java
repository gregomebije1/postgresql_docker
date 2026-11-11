package com.example.gateway.payment;

import com.example.gateway.idempotency.IdempotencyService;
import com.example.gateway.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class PaymentControllerIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired PaymentRepository payments;
    @Autowired OutboxRepository outbox;

    @MockitoBean IdempotencyService idempotency;

    @Test
    void createsPaymentThroughHttp() {
        when(idempotency.acquire("http-idem")).thenReturn(true);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "http-idem");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(
                "{\"reference\":\"http-pay\",\"amount\":25.00,\"currency\":\"GBP\"}", headers);

        ResponseEntity<PaymentResponse> response = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/payments",
                entity, PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payments.findByReference("http-pay")).isPresent();
        assertThat(outbox.count()).isEqualTo(1);
    }
}
