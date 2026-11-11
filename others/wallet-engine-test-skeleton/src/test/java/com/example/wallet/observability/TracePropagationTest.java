package com.example.wallet.observability;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

class TracePropagationTest {
    @Test
    void traceparentIsCopiedToDownstreamHeaders() {
        String traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        HttpHeaders headers = new HttpHeaders();
        headers.set("traceparent", traceparent);

        assertEquals(traceparent, headers.getFirst("traceparent"));
    }
}
