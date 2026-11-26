package com.gregomebije.wallet.service.compliance;

import java.math.BigDecimal;

public interface DailyVolumeRepository {
    BigDecimal getDailySuccessfulVolume(String userId);
}
