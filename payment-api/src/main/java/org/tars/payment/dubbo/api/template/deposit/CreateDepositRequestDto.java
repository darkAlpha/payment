package org.tars.payment.dubbo.api.template.deposit;
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
public class CreateDepositRequestDto implements Serializable {
    private String customerId;
    private String accountId;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private String currency;
    private int termMonths;
    private String description;
    private String idempotencyKey;
}
