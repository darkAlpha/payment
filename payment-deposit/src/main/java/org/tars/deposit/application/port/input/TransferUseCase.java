package org.tars.deposit.application.port.input;

import org.tars.deposit.application.dto.TransferCommand;
import org.tars.deposit.application.dto.TransferResult;

public interface TransferUseCase {
    TransferResult execute(TransferCommand command);
}
