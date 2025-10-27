package com.firefly.psps.dtos.customers;
import lombok.*;
@Data @Builder public class AttachPaymentMethodRequest {
    private String customerId;
    private String paymentMethodId;
}
