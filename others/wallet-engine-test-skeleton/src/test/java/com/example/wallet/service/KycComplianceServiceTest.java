package com.gregomebije.wallet.service;

import com.gregomebije.wallet.exception.Exceptions.ComplianceLimitExceededException;
import com.gregomebije.wallet.model.Models.ComplianceTier;
import com.gregomebije.wallet.service.compliance.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class KycComplianceServiceTest {
    private final SimpleDailyVolumeRepository repo = new SimpleDailyVolumeRepository();
    private final KycComplianceService service = new KycComplianceService(repo);

    @Test
    void tier1ShouldRejectAbove100Daily() {
        repo.set("u", new BigDecimal("90"));
        assertThrows(ComplianceLimitExceededException.class,
                () -> service.validate("u", ComplianceTier.TIER_1, new BigDecimal("10.01")));
    }

    @Test
    void tier1ExactlyAt100ShouldPass() {
        repo.set("u", new BigDecimal("90"));
        assertDoesNotThrow(() -> service.validate("u", ComplianceTier.TIER_1, new BigDecimal("10")));
    }

    @Test
    void tier2ShouldNotUseTier1Limit() {
        repo.set("u", new BigDecimal("10000"));
        assertDoesNotThrow(() -> service.validate("u", ComplianceTier.TIER_2, new BigDecimal("5000")));
    }

    @Test
    void decimalArithmeticShouldBeExact() {
        repo.set("u", new BigDecimal("99.99"));
        assertThrows(ComplianceLimitExceededException.class,
                () -> service.validate("u", ComplianceTier.TIER_1, new BigDecimal("0.02")));
    }
}
