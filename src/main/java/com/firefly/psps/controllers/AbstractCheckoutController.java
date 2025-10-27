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

import com.firefly.psps.dtos.checkout.*;
import com.firefly.psps.services.AbstractPspService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Abstract REST controller for checkout and payment intent operations.
 * 
 * Implementations can extend this to get standardized checkout endpoints.
 * Override methods to customize behavior or add additional endpoints.
 * 
 * Default endpoints:
 * - POST   /checkout/sessions - Create hosted checkout session
 * - GET    /checkout/sessions/{id} - Get checkout session
 * - POST   /checkout/payment-intents - Create payment intent (for client-side)
 * - GET    /checkout/payment-intents/{id} - Get payment intent
 * - PATCH  /checkout/payment-intents/{id} - Update payment intent
 */
public abstract class AbstractCheckoutController {

    protected final AbstractPspService pspService;

    protected AbstractCheckoutController(AbstractPspService pspService) {
        this.pspService = pspService;
    }

    /**
     * Create a hosted checkout session (redirect flow).
     * 
     * POST /api/checkout/sessions
     */
    @PostMapping("/sessions")
    public Mono<ResponseEntity<CheckoutSessionResponse>> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return pspService.createCheckoutSession(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get checkout session details.
     * 
     * GET /api/checkout/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<ResponseEntity<CheckoutSessionResponse>> getCheckoutSession(@PathVariable String sessionId) {
        return pspService.getCheckoutSession(sessionId)
                .map(ResponseEntity::ok);
    }

    /**
     * Create a payment intent (for client-side or mobile integration).
     * 
     * POST /api/checkout/payment-intents
     */
    @PostMapping("/payment-intents")
    public Mono<ResponseEntity<PaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        return pspService.createPaymentIntent(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get payment intent details.
     * 
     * GET /api/checkout/payment-intents/{intentId}
     */
    @GetMapping("/payment-intents/{intentId}")
    public Mono<ResponseEntity<PaymentIntentResponse>> getPaymentIntent(@PathVariable String intentId) {
        return pspService.getPaymentIntent(intentId)
                .map(ResponseEntity::ok);
    }

    /**
     * Update a payment intent.
     * 
     * PATCH /api/checkout/payment-intents/{intentId}
     */
    @PatchMapping("/payment-intents/{intentId}")
    public Mono<ResponseEntity<PaymentIntentResponse>> updatePaymentIntent(
            @PathVariable String intentId,
            @Valid @RequestBody UpdatePaymentIntentRequest request) {
        // Set the intent ID from path parameter if not already set
        if (request.getIntentId() == null) {
            request.setIntentId(intentId);
        }
        return pspService.updatePaymentIntent(request)
                .map(ResponseEntity::ok);
    }
}
