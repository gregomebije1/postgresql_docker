package com.gregomebije.wallet.api;

import com.gregomebije.wallet.exception.Exceptions.DuplicateRequestException;
import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.transfer.P2PTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({P2PTransferController.class, ApiExceptionHandler.class})
class P2PTransferControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean P2PTransferService service;

    @Test
    void shouldAcceptP2PTransfer() throws Exception {
        when(service.transfer(any(), eq("key"))).thenReturn(
                new P2PResponse("tx-123","sender","receiver",new BigDecimal("2500"),BigDecimal.ZERO));

        mockMvc.perform(post("/api/v1/transactions/p2p")
                .header("X-Idempotency-Key","key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"senderWalletId":"sender","receiverWalletId":"receiver","amount":2500,"currency":"USD","narration":"Dinner split"}
                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transaction_id").value("tx-123"));
    }

    @Test
    void shouldRejectNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/p2p")
                .header("X-Idempotency-Key","key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"senderWalletId":"sender","receiverWalletId":"receiver","amount":-100,"currency":"USD"}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateShouldReturn409() throws Exception {
        when(service.transfer(any(), eq("dup"))).thenThrow(new DuplicateRequestException());

        mockMvc.perform(post("/api/v1/transactions/p2p")
                .header("X-Idempotency-Key","dup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"senderWalletId":"sender","receiverWalletId":"receiver","amount":100,"currency":"USD"}
                """))
                .andExpect(status().isConflict());
    }
}
