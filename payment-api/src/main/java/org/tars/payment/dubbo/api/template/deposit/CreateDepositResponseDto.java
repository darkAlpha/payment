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
public class CreateDepositResponseDto implements Serializable {
    private String depositId;
    private String customerId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String message;
    private String createdAt;
}
