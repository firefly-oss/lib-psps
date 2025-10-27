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

package com.firefly.psps.controllers;

import com.firefly.psps.dtos.payments.*;
import com.firefly.psps.services.AbstractPspService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * Abstract REST controller for payment operations.
 * 
 * Implementations can extend this to get standardized payment endpoints.
 * Override methods to customize behavior or add additional endpoints.
 */
public abstract class AbstractPaymentController {

    protected final AbstractPspService pspService;

    protected AbstractPaymentController(AbstractPspService pspService) {
        this.pspService = pspService;
    }

    /**
     * Create a new payment.
     * 
     * POST /api/payments
     */
    @PostMapping
    public Mono<ResponseEntity<PaymentResponse>> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return pspService.createPayment(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get payment by ID.
     * 
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public Mono<ResponseEntity<PaymentResponse>> getPayment(@PathVariable String paymentId) {
        return pspService.getPayment(paymentId)
                .map(ResponseEntity::ok);
    }

    /**
     * Confirm a payment.
     * 
     * POST /api/payments/{paymentId}/confirm
     */
    @PostMapping("/{paymentId}/confirm")
    public Mono<ResponseEntity<PaymentResponse>> confirmPayment(
            @PathVariable String paymentId,
            @RequestBody(required = false) ConfirmPaymentRequest request) {
        
        if (request == null) {
            request = ConfirmPaymentRequest.builder()
                    .paymentId(paymentId)
                    .build();
        } else if (request.getPaymentId() == null) {
            request = ConfirmPaymentRequest.builder()
                    .paymentId(paymentId)
                    .paymentMethodId(request.getPaymentMethodId())
                    .returnUrl(request.getReturnUrl())
                    .build();
        }
        
        return pspService.confirmPayment(request)
                .map(ResponseEntity::ok);
    }

    /**
     * Capture a payment.
     * 
     * POST /api/payments/{paymentId}/capture
     */
    @PostMapping("/{paymentId}/capture")
    public Mono<ResponseEntity<PaymentResponse>> capturePayment(
            @PathVariable String paymentId,
            @RequestBody(required = false) CapturePaymentRequest request) {
        
        if (request == null) {
            request = CapturePaymentRequest.builder()
                    .paymentId(paymentId)
                    .build();
        }
        
        return pspService.capturePayment(request)
                .map(ResponseEntity::ok);
    }

    /**
     * Cancel a payment.
     * 
     * POST /api/payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    public Mono<ResponseEntity<PaymentResponse>> cancelPayment(@PathVariable String paymentId) {
        return pspService.cancelPayment(paymentId)
                .map(ResponseEntity::ok);
    }
}
