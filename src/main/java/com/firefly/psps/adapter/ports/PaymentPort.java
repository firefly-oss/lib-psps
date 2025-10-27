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

package com.firefly.psps.adapter.ports;

import com.firefly.psps.dtos.payments.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port interface for payment operations.
 * 
 * Defines the core payment lifecycle operations that all PSP implementations must support.
 */
public interface PaymentPort {

    /**
     * Create a payment intent.
     * 
     * A payment intent represents the intention to collect payment from a customer.
     * It tracks the lifecycle of the payment from creation through completion.
     *
     * @param request payment creation request
     * @return reactive publisher with payment response
     */
    Mono<ResponseEntity<PaymentResponse>> createPayment(CreatePaymentRequest request);

    /**
     * Retrieve a payment by its identifier.
     *
     * @param paymentId PSP-specific payment identifier
     * @return reactive publisher with payment details
     */
    Mono<ResponseEntity<PaymentResponse>> getPayment(String paymentId);

    /**
     * Confirm a payment intent.
     * 
     * Confirms that payment should be attempted with the provided payment method.
     * This may trigger 3D Secure or other authentication flows.
     *
     * @param request payment confirmation request
     * @return reactive publisher with updated payment response
     */
    Mono<ResponseEntity<PaymentResponse>> confirmPayment(ConfirmPaymentRequest request);

    /**
     * Capture a payment that was authorized but not yet captured.
     * 
     * Used in two-step payment flows where authorization and capture are separate.
     *
     * @param request capture request with payment ID and optional amount
     * @return reactive publisher with captured payment response
     */
    Mono<ResponseEntity<PaymentResponse>> capturePayment(CapturePaymentRequest request);

    /**
     * Cancel a payment intent.
     * 
     * Can only cancel payments that have not yet been successfully completed.
     *
     * @param paymentId payment identifier to cancel
     * @return reactive publisher with cancelled payment response
     */
    Mono<ResponseEntity<PaymentResponse>> cancelPayment(String paymentId);

    /**
     * List payments with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of payments
     */
    Mono<ResponseEntity<List<PaymentResponse>>> listPayments(ListPaymentsRequest request);

    /**
     * Update payment metadata or description.
     *
     * @param request update request with payment ID and fields to update
     * @return reactive publisher with updated payment response
     */
    Mono<ResponseEntity<PaymentResponse>> updatePayment(UpdatePaymentRequest request);
}
