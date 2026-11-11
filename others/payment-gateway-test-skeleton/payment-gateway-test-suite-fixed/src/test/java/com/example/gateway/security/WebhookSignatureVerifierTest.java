package com.example.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    @Test
    void acceptsValidSignature() {
        String body = "{\"reference\":\"pay-1\"}";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = org.apache.commons.codec.digest.HmacUtils.hmacSha256Hex(
                "secret", timestamp + "." + body);

        assertThat(verifier.verify(timestamp, signature, body, "secret")).isTrue();
    }

    @Test
    void rejectsWrongSignature() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        assertThat(verifier.verify(timestamp, "bad", "{}", "secret")).isFalse();
    }

    @Test
    void rejectsExpiredTimestamp() {
        String old = String.valueOf(System.currentTimeMillis() / 1000 - 301);
        String body = "{}";
        String signature = org.apache.commons.codec.digest.HmacUtils.hmacSha256Hex(
                "secret", old + "." + body);

        assertThat(verifier.verify(old, signature, body, "secret")).isFalse();
    }
}
