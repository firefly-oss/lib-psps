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

import com.firefly.psps.dtos.checkout.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * Port interface for hosted checkout sessions.
 * 
 * Provides a standardized way to create checkout/payment intent sessions
 * that redirect customers to the PSP's hosted payment page.
 * 
 * This abstracts provider-specific checkout flows (e.g., Stripe Checkout, 
 * Adyen Drop-in, PayPal Checkout) into a unified interface.
 */
public interface CheckoutPort {

    /**
     * Create a checkout session that redirects the customer to a hosted payment page.
     * 
     * The PSP will handle the payment UI, and redirect back to your success/cancel URLs.
     * 
     * @param request checkout session request with items, customer, and URLs
     * @return reactive publisher with session details including redirect URL
     */
    Mono<ResponseEntity<CheckoutSessionResponse>> createCheckoutSession(CreateCheckoutSessionRequest request);

    /**
     * Retrieve a checkout session by its identifier.
     *
     * @param sessionId checkout session identifier
     * @return reactive publisher with session details
     */
    Mono<ResponseEntity<CheckoutSessionResponse>> getCheckoutSession(String sessionId);

    /**
     * Expire/cancel a checkout session before completion.
     *
     * @param sessionId session identifier to expire
     * @return reactive publisher with updated session
     */
    Mono<ResponseEntity<CheckoutSessionResponse>> expireCheckoutSession(String sessionId);

    /**
     * Create a payment intent for client-side integration (e.g., mobile apps).
     * 
     * Returns a client secret that can be used with provider's client SDKs
     * to collect payment on the client side.
     *
     * @param request payment intent request
     * @return reactive publisher with intent details including client secret
     */
    Mono<ResponseEntity<PaymentIntentResponse>> createPaymentIntent(CreatePaymentIntentRequest request);

    /**
     * Retrieve a payment intent by its identifier.
     *
     * @param intentId payment intent identifier
     * @return reactive publisher with intent details
     */
    Mono<ResponseEntity<PaymentIntentResponse>> getPaymentIntent(String intentId);

    /**
     * Update a payment intent (e.g., change amount, metadata).
     *
     * @param request update request with intent ID and fields to change
     * @return reactive publisher with updated intent
     */
    Mono<ResponseEntity<PaymentIntentResponse>> updatePaymentIntent(UpdatePaymentIntentRequest request);

    /**
     * Cancel a payment intent.
     *
     * @param intentId payment intent identifier
     * @return reactive publisher with canceled intent
     */
    Mono<ResponseEntity<PaymentIntentResponse>> cancelPaymentIntent(String intentId);
}
