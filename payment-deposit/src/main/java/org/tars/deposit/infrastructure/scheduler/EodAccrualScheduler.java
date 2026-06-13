package org.tars.deposit.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.tars.deposit.application.dto.AccrualResult;
import org.tars.deposit.application.port.input.AccrualUseCase;

/**
 * Scheduler for End-of-Day interest accrual.
 * Runs daily at 23:59.
 */
@Component
@EnableScheduling
public class EodAccrualScheduler {

    private static final Logger log = LoggerFactory.getLogger(EodAccrualScheduler.class);

    private final AccrualUseCase accrualUseCase;

    public EodAccrualScheduler(AccrualUseCase accrualUseCase) {
        this.accrualUseCase = accrualUseCase;
    }

    @Scheduled(cron = "${payment.eod.cron:0 59 23 * * *}")
    public void runEodAccrual() {
        log.info("=== EOD ACCRUAL JOB STARTED ===");
        try {
            AccrualResult result = accrualUseCase.executeEod();
            log.info("=== EOD ACCRUAL JOB COMPLETED: processed={}, success={}, failures={}, duration={} ===",
                    result.processedAccounts(), result.successCount(), result.failureCount(), result.executionTime());
        } catch (Exception e) {
            log.error("=== EOD ACCRUAL JOB FAILED ===", e);
        }
    }
}
