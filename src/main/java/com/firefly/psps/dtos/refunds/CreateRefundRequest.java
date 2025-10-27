package com.firefly.psps.dtos.refunds;
import com.firefly.psps.domain.Money;
import lombok.*;
@Data @Builder public class CreateRefundRequest {
    private String paymentId;
    private Money amount;
    private String reason;
}
