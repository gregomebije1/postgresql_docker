package com.gregomebije.wallet.service;

import com.gregomebije.wallet.model.Models.*;
import com.gregomebije.wallet.service.history.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityHistoryServiceTest {
    @Test
    void combinesLedgerTransactionsWithUserMetadata() {
        ActivityLedgerClient ledger = mock(ActivityLedgerClient.class);
        UserMetadataRepository metadata = mock(UserMetadataRepository.class);

        when(ledger.getTransactions("wallet-1")).thenReturn(List.of(
                new LedgerTransaction("tx-1","AIRTIME",new BigDecimal("1000")),
                new LedgerTransaction("tx-2","P2P",new BigDecimal("2500"))));
        when(metadata.findByWalletId("wallet-1")).thenReturn(new UserMetadata("wallet-1","usr-1"));

        var result = new ActivityHistoryService(ledger, metadata).getActivity("wallet-1");

        assertEquals(2, result.size());
        assertEquals("usr-1", result.get(0).userId());
        assertEquals("AIRTIME", result.get(0).type());
    }
}
