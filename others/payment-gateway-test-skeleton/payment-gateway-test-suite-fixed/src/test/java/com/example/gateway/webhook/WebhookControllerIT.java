package com.example.gateway.webhook;

import com.example.gateway.idempotency.IdempotencyService;
import com.example.gateway.payment.*;
import com.example.gateway.outbox.OutboxRepository;
import org.apache.commons.codec.digest.HmacUtils;
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
class WebhookControllerIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired PaymentRepository payments;

    @MockitoBean IdempotencyService idempotency;

    @Test
    void acceptsValidWebhookAndUpdatesPayment() {
        when(idempotency.acquire("create-webhook-pay")).thenReturn(true);

        payments.save(new Payment("webhook-pay", new BigDecimal("15.00"), "GBP"));

        String body = "{\"reference\":\"webhook-pay\"}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = HmacUtils.hmacSha256Hex(
                "integration-secret", timestamp + "." + body);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Signature-Timestamp", timestamp);
        headers.set("X-Signature", signature);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/settlements/webhooks/stripe",
                new HttpEntity<>(body, headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payments.findByReference("webhook-pay").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void rejectsInvalidWebhook() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Signature-Timestamp",
                String.valueOf(System.currentTimeMillis() / 1000));
        headers.set("X-Signature", "invalid");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/settlements/webhooks/stripe",
                new HttpEntity<>("{\"reference\":\"missing\"}", headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
