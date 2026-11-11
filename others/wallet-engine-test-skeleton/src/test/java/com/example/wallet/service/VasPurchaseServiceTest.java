package com.gregomebije.wallet.service;

import com.gregomebije.wallet.exception.Exceptions.*;
import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.vas.*;
import com.gregomebije.wallet.service.ledger.*;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VasPurchaseServiceTest {
    private VasPurchaseRequest request() {
        return new VasPurchaseRequest("wallet-1", VasType.AIRTIME, "MTN_NG",
                new BigDecimal("1000"), "+2348030000001");
    }

    @Test
    void successCapturesHold() {
        LedgerService ledger = mock(LedgerService.class);
        UtilityProviderClient provider = mock(UtilityProviderClient.class);
        when(ledger.placeHold("wallet-1", new BigDecimal("1000"))).thenReturn("hold-1");
        when(provider.purchase(any())).thenReturn(new UtilityPurchaseResult(true, "p-1"));

        new VasPurchaseService(ledger, provider).purchase(request());

        InOrder order = inOrder(ledger, provider);
        order.verify(ledger).placeHold("wallet-1", new BigDecimal("1000"));
        order.verify(provider).purchase(any());
        order.verify(ledger).captureHold("hold-1");
        verify(ledger, never()).releaseHold(any());
    }

    @Test
    void providerFailureReleasesHold() {
        LedgerService ledger = mock(LedgerService.class);
        UtilityProviderClient provider = mock(UtilityProviderClient.class);
        when(ledger.placeHold(anyString(), any())).thenReturn("hold-1");
        when(provider.purchase(any())).thenThrow(new ProviderTimeoutException());

        assertThrows(VasPurchaseFailedException.class,
                () -> new VasPurchaseService(ledger, provider).purchase(request()));

        verify(ledger).releaseHold("hold-1");
        verify(ledger, never()).captureHold(any());
    }

    @Test
    void ledgerHoldFailureDoesNotCallProvider() {
        LedgerService ledger = mock(LedgerService.class);
        UtilityProviderClient provider = mock(UtilityProviderClient.class);
        when(ledger.placeHold(anyString(), any())).thenThrow(new InsufficientFundsException());

        assertThrows(InsufficientFundsException.class,
                () -> new VasPurchaseService(ledger, provider).purchase(request()));

        verifyNoInteractions(provider);
    }
}
