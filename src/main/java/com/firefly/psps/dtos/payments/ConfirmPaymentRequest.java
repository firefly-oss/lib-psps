/*
 * Copyright 2025 Firefly Software Solutions Inc
 */
package com.firefly.psps.dtos.payments;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ConfirmPaymentRequest {
    private String paymentId;
    private String paymentMethodId;
    private String returnUrl;
}
