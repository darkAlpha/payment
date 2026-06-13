package org.tars.deposit.application.port.input;

import org.tars.deposit.application.dto.CreateDepositCommand;
import org.tars.deposit.application.dto.DepositResult;

/**
 * Input port - use case for creating a deposit.
 * Follows Interface Segregation Principle.
 */
public interface CreateDepositUseCase {

    DepositResult execute(CreateDepositCommand command);
}

