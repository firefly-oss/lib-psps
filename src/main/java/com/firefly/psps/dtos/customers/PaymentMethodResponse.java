package com.firefly.psps.dtos.customers;
import com.firefly.psps.domain.PaymentMethodType;
import lombok.*;
@Data @Builder public class PaymentMethodResponse {
    private String paymentMethodId;
    private PaymentMethodType type;
    private Object details;
}
