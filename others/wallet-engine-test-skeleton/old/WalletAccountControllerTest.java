package com.gregomebije.wallet;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.account.WalletAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletAccountController.class)
class WalletAccountControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean WalletAccountService service;

    @Test
    void shouldProvisionWalletAccount() throws Exception {
        //when(service.provision(any())).thenReturn(
        //        new WalletAccountResponse("wlt-test","acc-test","usr_8821","USD","ACTIVE", Instant.parse("2026-09-19T12:55:00Z")));

        when(service.provision(any(WalletAccountRequest.class))).thenReturn(
        new WalletAccountResponse("wlt-test","acc-test","usr_8821","USD","ACTIVE", Instant.parse("2026-09-19T12:55:00Z")));

   
        mockMvc.perform(post("/api/v1/wallets/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"usr_8821","complianceTier":"TIER_1","currency":"USD"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.walletId").value("wlt-test"));
    }

    @Test
    void shouldRejectMissingUserId() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"complianceTier":"TIER_1","currency":"USD"}
                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectInvalidCurrency() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"usr_1","complianceTier":"TIER_1","currency":""}
                """))
                .andExpect(status().isBadRequest());
    }
}


