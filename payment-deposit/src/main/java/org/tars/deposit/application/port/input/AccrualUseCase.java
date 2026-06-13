package org.tars.deposit.application.port.input;

import org.tars.deposit.application.dto.AccrualResult;

/**
 * EOD use case: calculate and apply daily interest accrual for all active deposits.
 */
public interface AccrualUseCase {
    AccrualResult executeEod();
}
