package com.gregomebije.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Import all your individual infrastructure packages cleanl

import com.gregomebije.wallet.service.funding.PaymentProviderRouter;
import com.gregomebije.wallet.service.ledger.LedgerClient;
import com.gregomebije.wallet.service.notification.NotificationPublisher;
import com.gregomebije.wallet.service.notification.RetryQueue;
import com.gregomebije.wallet.service.history.ActivityLedgerClient;
import com.gregomebije.wallet.service.compliance.DailyVolumeRepository;
import com.gregomebije.wallet.service.idempotency.DistributedLockService;
import com.gregomebije.wallet.service.ledger.LedgerService;
import com.gregomebije.wallet.service.vas.UtilityProviderClient;

@SpringBootTest
class BankApplicationTests {

	//Not yet implemented or marked with @Service, @Component or @Repository
    @MockitoBean private LedgerClient ledgerClient;
    @MockitoBean private PaymentProviderRouter paymentProviderRouter;
    @MockitoBean private NotificationPublisher notificationPublisher;
    @MockitoBean private RetryQueue retryQueue;
    @MockitoBean private LedgerService ledgerService;
    @MockitoBean private UtilityProviderClient utilityProviderClient; //require an actual live internet connection
    @MockitoBean private ActivityLedgerClient activityLedgerClient;
     @MockitoBean private DailyVolumeRepository dailyVolumeRepository;
    @MockitoBean private DistributedLockService distributedLockService;

    @Test
    void contextLoads() {
        
    }
}
