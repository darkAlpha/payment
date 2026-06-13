package org.tars.deposit.application.port.input;

import org.tars.deposit.application.dto.CloseDepositCommand;
import org.tars.deposit.application.dto.CloseDepositResult;

public interface CloseDepositUseCase {
    CloseDepositResult execute(CloseDepositCommand command);
}
