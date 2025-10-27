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

package com.firefly.psps.services;

import com.firefly.psps.adapter.PspAdapter;
import com.firefly.psps.dtos.checkout.*;
import com.firefly.psps.dtos.customers.*;
import com.firefly.psps.dtos.payments.*;
import com.firefly.psps.dtos.refunds.*;
import com.firefly.psps.dtos.subscriptions.*;
import com.firefly.psps.exceptions.PspException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Abstract base service providing common PSP operations.
 * 
 * Implementations should extend this class and inject their specific PspAdapter.
 * This provides a standardized service layer between controllers and adapters.
 */
public abstract class AbstractPspService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final PspAdapter pspAdapter;

    protected AbstractPspService(PspAdapter pspAdapter) {
        this.pspAdapter = pspAdapter;
    }

    // ========== Payment Operations ==========

    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        logger.info("Creating payment for customer: {}", request.getCustomerId());
        return pspAdapter.payments()
                .createPayment(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Payment created: {}", response.getPaymentId()))
                .doOnError(error -> logger.error("Payment creation failed", error))
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentResponse> getPayment(String paymentId) {
        logger.debug("Retrieving payment: {}", paymentId);
        return pspAdapter.payments()
                .getPayment(paymentId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentResponse> confirmPayment(ConfirmPaymentRequest request) {
        logger.info("Confirming payment: {}", request.getPaymentId());
        return pspAdapter.payments()
                .confirmPayment(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Payment confirmed: {}", response.getPaymentId()))
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentResponse> capturePayment(CapturePaymentRequest request) {
        logger.info("Capturing payment: {}", request.getPaymentId());
        return pspAdapter.payments()
                .capturePayment(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Payment captured: {}", response.getPaymentId()))
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentResponse> cancelPayment(String paymentId) {
        logger.info("Canceling payment: {}", paymentId);
        return pspAdapter.payments()
                .cancelPayment(paymentId)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Payment canceled: {}", response.getPaymentId()))
                .onErrorMap(this::handleException);
    }

    // ========== Refund Operations ==========

    public Mono<RefundResponse> createRefund(CreateRefundRequest request) {
        logger.info("Creating refund for payment: {}", request.getPaymentId());
        return pspAdapter.refunds()
                .createRefund(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Refund created: {}", response.getRefundId()))
                .onErrorMap(this::handleException);
    }

    public Mono<RefundResponse> getRefund(String refundId) {
        logger.debug("Retrieving refund: {}", refundId);
        return pspAdapter.refunds()
                .getRefund(refundId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    // ========== Subscription Operations ==========

    public Mono<SubscriptionResponse> createSubscription(CreateSubscriptionRequest request) {
        logger.info("Creating subscription for customer: {}", request.getCustomerId());
        return pspAdapter.subscriptions()
                .createSubscription(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Subscription created: {}", response.getSubscriptionId()))
                .onErrorMap(this::handleException);
    }

    public Mono<SubscriptionResponse> getSubscription(String subscriptionId) {
        logger.debug("Retrieving subscription: {}", subscriptionId);
        return pspAdapter.subscriptions()
                .getSubscription(subscriptionId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<SubscriptionResponse> cancelSubscription(CancelSubscriptionRequest request) {
        logger.info("Canceling subscription: {}", request.getSubscriptionId());
        return pspAdapter.subscriptions()
                .cancelSubscription(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Subscription canceled: {}", response.getSubscriptionId()))
                .onErrorMap(this::handleException);
    }

    // ========== Checkout Operations ==========

    public Mono<CheckoutSessionResponse> createCheckoutSession(CreateCheckoutSessionRequest request) {
        logger.info("Creating checkout session for mode: {}", request.getMode());
        return pspAdapter.checkout()
                .createCheckoutSession(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Checkout session created: {}", response.getSessionId()))
                .onErrorMap(this::handleException);
    }

    public Mono<CheckoutSessionResponse> getCheckoutSession(String sessionId) {
        logger.debug("Retrieving checkout session: {}", sessionId);
        return pspAdapter.checkout()
                .getCheckoutSession(sessionId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentIntentResponse> createPaymentIntent(CreatePaymentIntentRequest request) {
        logger.info("Creating payment intent for customer: {}", request.getCustomerId());
        return pspAdapter.checkout()
                .createPaymentIntent(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Payment intent created: {}", response.getIntentId()))
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentIntentResponse> getPaymentIntent(String intentId) {
        logger.debug("Retrieving payment intent: {}", intentId);
        return pspAdapter.checkout()
                .getPaymentIntent(intentId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<PaymentIntentResponse> updatePaymentIntent(UpdatePaymentIntentRequest request) {
        logger.info("Updating payment intent: {}", request.getIntentId());
        return pspAdapter.checkout()
                .updatePaymentIntent(request)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    // ========== Customer Operations ==========

    public Mono<CustomerResponse> createCustomer(CreateCustomerRequest request) {
        logger.info("Creating customer: {}", request.getCustomerInfo() != null ? request.getCustomerInfo().getEmail() : "unknown");
        return pspAdapter.customers()
                .createCustomer(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Customer created: {}", response.getCustomerId()))
                .onErrorMap(this::handleException);
    }

    public Mono<CustomerResponse> getCustomer(String customerId) {
        logger.debug("Retrieving customer: {}", customerId);
        return pspAdapter.customers()
                .getCustomer(customerId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<CustomerResponse> updateCustomer(UpdateCustomerRequest request) {
        logger.info("Updating customer: {}", request.getCustomerId());
        return pspAdapter.customers()
                .updateCustomer(request)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<Void> deleteCustomer(String customerId) {
        logger.info("Deleting customer: {}", customerId);
        return pspAdapter.customers()
                .deleteCustomer(customerId)
                .doOnSuccess(v -> logger.info("Customer deleted: {}", customerId))
                .onErrorResume(error -> {
                    logger.error("Customer deletion failed: {}", customerId, error);
                    return Mono.error(handleException(error));
                });
    }

    public Mono<PaymentMethodResponse> attachPaymentMethod(AttachPaymentMethodRequest request) {
        logger.info("Attaching payment method to customer: {}", request.getCustomerId());
        return pspAdapter.customers()
                .attachPaymentMethod(request)
                .map(ResponseEntity::getBody)
                .doOnSuccess(response -> logger.info("Payment method attached: {}", response.getPaymentMethodId()))
                .onErrorMap(this::handleException);
    }

    public Mono<List<PaymentMethodResponse>> listPaymentMethods(String customerId) {
        logger.debug("Listing payment methods for customer: {}", customerId);
        return pspAdapter.customers()
                .listPaymentMethods(customerId)
                .map(ResponseEntity::getBody)
                .onErrorMap(this::handleException);
    }

    public Mono<Void> detachPaymentMethod(String customerId, String paymentMethodId) {
        logger.info("Detaching payment method {} from customer: {}", paymentMethodId, customerId);
        return pspAdapter.customers()
                .detachPaymentMethod(customerId, paymentMethodId)
                .doOnSuccess(v -> logger.info("Payment method detached: {}", paymentMethodId))
                .onErrorResume(error -> {
                    logger.error("Payment method detachment failed", error);
                    return Mono.error(handleException(error));
                });
    }

    // ========== Helper Methods ==========

    /**
     * Handle exceptions from PSP operations.
     * Subclasses can override to provide custom exception handling.
     */
    protected Throwable handleException(Throwable throwable) {
        if (throwable instanceof PspException) {
            return throwable;
        }
        logger.error("Unexpected error in PSP operation", throwable);
        return new PspException("PSP operation failed: " + throwable.getMessage(), throwable);
    }

    /**
     * Get the PSP adapter instance.
     */
    protected PspAdapter getPspAdapter() {
        return pspAdapter;
    }

    /**
     * Get the provider name.
     */
    public String getProviderName() {
        return pspAdapter.getProviderName();
    }
}
