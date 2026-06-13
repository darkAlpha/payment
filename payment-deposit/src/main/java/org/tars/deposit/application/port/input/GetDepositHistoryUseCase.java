package org.tars.deposit.application.port.input;

import org.tars.deposit.application.dto.DepositResult;

import java.util.List;

/**
 * Input port - use case for retrieving deposit history.
 * Follows Interface Segregation Principle.
 */
public interface GetDepositHistoryUseCase {

    List<DepositResult> execute(String customerId);
}

