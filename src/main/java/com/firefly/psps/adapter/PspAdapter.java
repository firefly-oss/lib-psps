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

package com.firefly.psps.adapter;

import com.firefly.psps.adapter.ports.*;

/**
 * Main Payment Service Provider (PSP) adapter interface.
 * 
 * This is the primary port in the hexagonal architecture that defines
 * the standardized contract for all PSP implementations (Stripe, Adyen, etc.).
 * 
 * Each implementation should provide access to specific operation ports
 * that handle different aspects of payment processing.
 */
public interface PspAdapter {

    /**
     * Get the payment operations port.
     * Handles payment intent creation, confirmation, capture, and cancellation.
     *
     * @return PaymentPort instance
     */
    PaymentPort payments();

    /**
     * Get the refund operations port.
     * Handles full and partial refunds of payments.
     *
     * @return RefundPort instance
     */
    RefundPort refunds();

    /**
     * Get the payout operations port.
     * Handles transferring funds to external accounts.
     *
     * @return PayoutPort instance
     */
    PayoutPort payouts();

    /**
     * Get the customer management port.
     * Handles customer creation, retrieval, and payment method management.
     *
     * @return CustomerPort instance
     */
    CustomerPort customers();

    /**
     * Get the dispute management port.
     * Handles chargebacks and dispute resolution.
     *
     * @return DisputePort instance
     */
    DisputePort disputes();

    /**
     * Get the subscription and billing port.
     * Handles recurring subscriptions, pricing plans, and scheduled billing.
     *
     * @return SubscriptionPort instance
     */
    SubscriptionPort subscriptions();

    /**
     * Get the checkout session port.
     * Handles hosted checkout pages and payment intents for redirect/client-side flows.
     *
     * @return CheckoutPort instance
     */
    CheckoutPort checkout();

    /**
     * Get the provider-specific operations port.
     * Allows access to PSP-specific features not covered by standard ports.
     *
     * @return ProviderSpecificPort instance
     */
    ProviderSpecificPort providerSpecific();

    /**
     * Get the reconciliation port.
     * Handles payment reconciliation, settlement reports and discrepancy detection.
     *
     * @return ReconciliationPort instance
     */
    ReconciliationPort reconciliation();

    /**
     * Get the PSP provider name.
     *
     * @return provider identifier (e.g., "stripe", "adyen", "braintree")
     */
    String getProviderName();

    /**
     * Health check to verify PSP connectivity and credentials.
     *
     * @return true if PSP is reachable and authenticated, false otherwise
     */
    boolean isHealthy();
}
