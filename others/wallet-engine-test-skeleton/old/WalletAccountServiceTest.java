package com.gregomebije.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceUnitTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerClient ledgerClient;

    @InjectMocks
    private WalletService walletService;

    @Test
    void shouldSuccessfullyProcessWalletOperation() {
        // Arrange
        String customerId = "cust_uuid_A";
        BigDecimal amount = BigDecimal.valueOf(100.00);

        // Act & Assert (verify service behavior and mock interactions)
        assertThatCode(() -> walletService.fundWalletLocally(customerId, amount))
                .doesNotThrowAnyException();
        
        verify(walletRepository, times(1)).incrementBalance(customerId, amount);
    }
}