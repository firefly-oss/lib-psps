package com.firefly.psps.dtos.payments;
import com.firefly.psps.domain.Money;
import lombok.*;
@Data @Builder public class CapturePaymentRequest {
    private String paymentId;
    private Money amount;
}
