package org.tars.payment.dubbo.api.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryRequestDto implements Serializable {
    private String accountId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String entryType;      // DEBIT, CREDIT
    private String operationType;  // DEPOSIT_CREATE, DEPOSIT_CLOSE, TRANSFER, ACCRUAL
    private String description;
    private String referenceId;
}
