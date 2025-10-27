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

import com.firefly.psps.dtos.subscriptions.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port interface for subscription and recurring billing operations.
 * 
 * Handles subscription lifecycle, pricing plans, and scheduled billing.
 */
public interface SubscriptionPort {

    /**
     * Create a pricing plan/product for subscriptions.
     *
     * @param request pricing plan creation request
     * @return reactive publisher with pricing plan response
     */
    Mono<ResponseEntity<PricingPlanResponse>> createPricingPlan(CreatePricingPlanRequest request);

    /**
     * Retrieve a pricing plan by its identifier.
     *
     * @param planId pricing plan identifier
     * @return reactive publisher with pricing plan details
     */
    Mono<ResponseEntity<PricingPlanResponse>> getPricingPlan(String planId);

    /**
     * Update a pricing plan.
     *
     * @param request pricing plan update request
     * @return reactive publisher with updated pricing plan response
     */
    Mono<ResponseEntity<PricingPlanResponse>> updatePricingPlan(UpdatePricingPlanRequest request);

    /**
     * List all pricing plans with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of pricing plans
     */
    Mono<ResponseEntity<List<PricingPlanResponse>>> listPricingPlans(ListPricingPlansRequest request);

    /**
     * Create a subscription for a customer.
     *
     * @param request subscription creation request
     * @return reactive publisher with subscription response
     */
    Mono<ResponseEntity<SubscriptionResponse>> createSubscription(CreateSubscriptionRequest request);

    /**
     * Retrieve a subscription by its identifier.
     *
     * @param subscriptionId subscription identifier
     * @return reactive publisher with subscription details
     */
    Mono<ResponseEntity<SubscriptionResponse>> getSubscription(String subscriptionId);

    /**
     * Update a subscription (change plan, quantity, etc.).
     *
     * @param request subscription update request
     * @return reactive publisher with updated subscription response
     */
    Mono<ResponseEntity<SubscriptionResponse>> updateSubscription(UpdateSubscriptionRequest request);

    /**
     * Cancel a subscription.
     * 
     * Can cancel immediately or at period end depending on the request.
     *
     * @param request cancellation request with subscription ID and options
     * @return reactive publisher with canceled subscription response
     */
    Mono<ResponseEntity<SubscriptionResponse>> cancelSubscription(CancelSubscriptionRequest request);

    /**
     * Pause a subscription (if supported by the PSP).
     *
     * @param subscriptionId subscription identifier to pause
     * @return reactive publisher with paused subscription response
     */
    Mono<ResponseEntity<SubscriptionResponse>> pauseSubscription(String subscriptionId);

    /**
     * Resume a paused subscription.
     *
     * @param subscriptionId subscription identifier to resume
     * @return reactive publisher with resumed subscription response
     */
    Mono<ResponseEntity<SubscriptionResponse>> resumeSubscription(String subscriptionId);

    /**
     * List subscriptions for a customer.
     *
     * @param customerId customer identifier
     * @return reactive publisher with list of subscriptions
     */
    Mono<ResponseEntity<List<SubscriptionResponse>>> listSubscriptionsForCustomer(String customerId);

    /**
     * List all subscriptions with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of subscriptions
     */
    Mono<ResponseEntity<List<SubscriptionResponse>>> listSubscriptions(ListSubscriptionsRequest request);

    /**
     * Retrieve upcoming invoice for a subscription.
     *
     * @param subscriptionId subscription identifier
     * @return reactive publisher with upcoming invoice details
     */
    Mono<ResponseEntity<InvoiceResponse>> getUpcomingInvoice(String subscriptionId);

    /**
     * List invoices for a subscription.
     *
     * @param subscriptionId subscription identifier
     * @return reactive publisher with list of invoices
     */
    Mono<ResponseEntity<List<InvoiceResponse>>> listInvoicesForSubscription(String subscriptionId);
}
