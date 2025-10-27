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

import com.firefly.psps.dtos.refunds.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port interface for refund operations.
 * 
 * Handles full and partial refunds of successful payments.
 */
public interface RefundPort {

    /**
     * Create a refund for a payment.
     * 
     * Can refund the full amount or a partial amount if the PSP supports it.
     *
     * @param request refund creation request
     * @return reactive publisher with refund response
     */
    Mono<ResponseEntity<RefundResponse>> createRefund(CreateRefundRequest request);

    /**
     * Retrieve a refund by its identifier.
     *
     * @param refundId PSP-specific refund identifier
     * @return reactive publisher with refund details
     */
    Mono<ResponseEntity<RefundResponse>> getRefund(String refundId);

    /**
     * Cancel a pending refund (if supported by the PSP).
     *
     * @param refundId refund identifier to cancel
     * @return reactive publisher with cancelled refund response
     */
    Mono<ResponseEntity<RefundResponse>> cancelRefund(String refundId);

    /**
     * List refunds for a specific payment.
     *
     * @param paymentId payment identifier
     * @return reactive publisher with list of refunds
     */
    Mono<ResponseEntity<List<RefundResponse>>> listRefundsForPayment(String paymentId);

    /**
     * List all refunds with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of refunds
     */
    Mono<ResponseEntity<List<RefundResponse>>> listRefunds(ListRefundsRequest request);
}
