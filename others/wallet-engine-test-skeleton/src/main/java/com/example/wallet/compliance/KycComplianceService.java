package com.gregomebije.wallet.service.compliance;

import com.gregomebije.wallet.exception.Exceptions.ComplianceLimitExceededException;
import com.gregomebije.wallet.model.Models.ComplianceTier;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class KycComplianceService {
    private final DailyVolumeRepository repo;

    public KycComplianceService(DailyVolumeRepository repo) { 
        this.repo = repo; 
    }

    public void validate(String userId, ComplianceTier tier, BigDecimal amount) {
        if (tier == ComplianceTier.TIER_1 &&
                repo.getDailySuccessfulVolume(userId).add(amount).compareTo(new BigDecimal("100.00")) > 0) {
            throw new ComplianceLimitExceededException();
        }
    }
}
