package com.gregomebije.wallet.api;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.funding.FundingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FundingController.class)
class FundingControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean FundingService service;

    @Test
    void fundingIntentShouldReturnPendingProvider() throws Exception {
        when(service.createIntent(any())).thenReturn(
                new FundingIntentResponse("fnd-1","wallet-1",new BigDecimal("5000"),
                        "USD",FundingStatus.PENDING_PROVIDER,"secret","https://checkout.example"));

        mockMvc.perform(post("/api/v1/wallets/funding/intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"walletId":"wallet-1","amount":5000,"currency":"USD","paymentMethod":"CREDIT_CARD","provider":"STRIPE"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PROVIDER"));
    }
}
