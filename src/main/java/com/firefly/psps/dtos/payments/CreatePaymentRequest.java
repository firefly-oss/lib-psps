/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.psps.dtos.payments;

import com.firefly.psps.domain.CustomerInfo;
import com.firefly.psps.domain.Money;
import com.firefly.psps.domain.PaymentMethodType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a payment.
 */
@Data
@Builder
public class CreatePaymentRequest {

    @NotNull
    private Money amount;

    private String customerId;
    private CustomerInfo customerInfo;
    private String paymentMethodId;
    private PaymentMethodType paymentMethodType;
    
    private String description;
    private String statementDescriptor;
    
    /**
     * Whether to capture the payment immediately or just authorize it.
     * If false, the payment must be explicitly captured later.
     */
    @Builder.Default
    private boolean captureImmediately = true;
    
    /**
     * Return URL for redirect-based payment methods (e.g., 3D Secure).
     */
    private String returnUrl;
    
    /**
     * Arbitrary metadata for the payment.
     */
    private Map<String, String> metadata;
    
    /**
     * Idempotency key to prevent duplicate payments.
     */
    private String idempotencyKey;
}
