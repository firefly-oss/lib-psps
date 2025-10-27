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

package com.firefly.psps.validation;

import com.firefly.psps.domain.Currency;
import com.firefly.psps.domain.Money;
import com.firefly.psps.domain.PaymentMethodType;
import com.firefly.psps.dtos.payments.CreatePaymentRequest;
import com.firefly.psps.dtos.refunds.CreateRefundRequest;
import com.firefly.psps.dtos.subscriptions.CreateSubscriptionRequest;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Validator for PSP operations before sending requests to the provider.
 * 
 * Pre-validates requests to:
 * - Save PSP API calls (reduce costs)
 * - Fail fast with clear error messages
 * - Apply business rules consistently
 * - Prevent predictable errors
 * 
 * Implementations should be PSP-specific as each provider has different:
 * - Supported currencies
 * - Payment limits
 * - Payment method restrictions
 * - Region-specific rules
 */
public interface PaymentValidator {

    /**
     * Validate a payment creation request before sending to PSP.
     *
     * @param request payment creation request
     * @return validation result with any errors
     */
    Mono<ValidationResult> validateCreatePayment(CreatePaymentRequest request);

    /**
     * Validate a refund creation request.
     *
     * @param request refund creation request
     * @return validation result
     */
    Mono<ValidationResult> validateCreateRefund(CreateRefundRequest request);

    /**
     * Validate a subscription creation request.
     *
     * @param request subscription creation request
     * @return validation result
     */
    Mono<ValidationResult> validateCreateSubscription(CreateSubscriptionRequest request);

    /**
     * Check if a currency is supported by this PSP.
     *
     * @param currency currency to check
     * @return true if supported
     */
    boolean isCurrencySupported(Currency currency);

    /**
     * Check if a payment method is supported by this PSP.
     *
     * @param paymentMethodType payment method type
     * @return true if supported
     */
    boolean isPaymentMethodSupported(PaymentMethodType paymentMethodType);

    /**
     * Check if amount is within PSP limits.
     *
     * @param amount amount to validate
     * @param currency currency of the amount
     * @return true if within limits
     */
    boolean isAmountWithinLimits(Money amount, Currency currency);

    /**
     * Get minimum amount for a currency.
     *
     * @param currency currency
     * @return minimum amount
     */
    BigDecimal getMinimumAmount(Currency currency);

    /**
     * Get maximum amount for a currency.
     *
     * @param currency currency
     * @return maximum amount
     */
    BigDecimal getMaximumAmount(Currency currency);

    /**
     * Get supported currencies for this PSP.
     *
     * @return set of supported currencies
     */
    Set<Currency> getSupportedCurrencies();

    /**
     * Get supported payment methods for this PSP.
     *
     * @return set of supported payment method types
     */
    Set<PaymentMethodType> getSupportedPaymentMethods();

    /**
     * Validation result containing errors and warnings.
     */
    interface ValidationResult {
        boolean isValid();
        java.util.List<ValidationError> getErrors();
        java.util.List<ValidationWarning> getWarnings();
        
        static ValidationResult success() {
            return new DefaultValidationResult(true, java.util.List.of(), java.util.List.of());
        }
        
        static ValidationResult failure(java.util.List<ValidationError> errors) {
            return new DefaultValidationResult(false, errors, java.util.List.of());
        }
        
        static ValidationResult withWarnings(java.util.List<ValidationWarning> warnings) {
            return new DefaultValidationResult(true, java.util.List.of(), warnings);
        }
    }

    /**
     * Validation error with field and message.
     *
     * @param field the field that failed validation
     * @param message the error message
     * @param code the error code
     */
    record ValidationError(String field, String message, String code) {}

    /**
     * Validation warning (non-blocking).
     *
     * @param field the field with the warning
     * @param message the warning message
     */
    record ValidationWarning(String field, String message) {}

    /**
     * Default implementation of ValidationResult.
     *
     * @param isValid whether validation passed
     * @param errors list of validation errors
     * @param warnings list of validation warnings
     */
    record DefaultValidationResult(
            boolean isValid,
            java.util.List<ValidationError> errors,
            java.util.List<ValidationWarning> warnings
    ) implements ValidationResult {
        @Override
        public java.util.List<ValidationError> getErrors() {
            return errors;
        }

        @Override
        public java.util.List<ValidationWarning> getWarnings() {
            return warnings;
        }
    }
}
