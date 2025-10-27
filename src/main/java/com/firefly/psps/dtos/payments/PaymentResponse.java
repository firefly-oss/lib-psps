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

import com.firefly.psps.domain.Money;
import com.firefly.psps.domain.PaymentMethodType;
import com.firefly.psps.domain.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO containing payment details.
 */
@Data
@Builder
public class PaymentResponse {

    private String paymentId;
    private Money amount;
    private Money amountCaptured;
    private Money amountRefunded;
    
    private PaymentStatus status;
    private PaymentMethodType paymentMethodType;
    private String paymentMethodId;
    
    private String customerId;
    private String description;
    private String statementDescriptor;
    
    private String clientSecret;
    private String redirectUrl;
    
    private Instant createdAt;
    private Instant updatedAt;
    
    private Map<String, String> metadata;
    private String providerPaymentId;
    private Object providerRawResponse;
}
