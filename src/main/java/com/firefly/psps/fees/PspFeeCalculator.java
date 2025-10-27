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

package com.firefly.psps.fees;

import com.firefly.psps.domain.Currency;
import com.firefly.psps.domain.Money;
import com.firefly.psps.domain.PaymentMethodType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Calculator for PSP fees and costs.
 * 
 * Helps track and optimize PSP costs by:
 * - Calculating fees per transaction
 * - Comparing costs across PSPs
 * - Supporting routing decisions
 * - Generating cost reports
 * 
 * Each PSP has different fee structures:
 * - Flat fee per transaction
 * - Percentage of transaction amount
 * - Minimum/maximum fee caps
 * - Currency-specific fees
 * - Payment method surcharges
 * - Volume discounts
 */
public interface PspFeeCalculator {

    /**
     * Calculate PSP fee for a payment.
     *
     * @param providerName PSP provider name
     * @param amount payment amount
     * @param paymentMethodType payment method
     * @return calculated fee
     */
    Money calculatePaymentFee(
            String providerName, 
            Money amount, 
            PaymentMethodType paymentMethodType);

    /**
     * Calculate net amount after PSP fees.
     *
     * @param providerName PSP provider name
     * @param grossAmount gross payment amount
     * @param paymentMethodType payment method
     * @return net amount (gross - fees)
     */
    Money calculateNetAmount(
            String providerName, 
            Money grossAmount, 
            PaymentMethodType paymentMethodType);

    /**
     * Calculate refund fee.
     * Some PSPs charge for refunds, others don't.
     *
     * @param providerName PSP provider name
     * @param refundAmount amount being refunded
     * @return refund processing fee
     */
    Money calculateRefundFee(String providerName, Money refundAmount);

    /**
     * Calculate chargeback fee.
     * Most PSPs charge a flat fee for chargebacks.
     *
     * @param providerName PSP provider name
     * @param currency currency of the transaction
     * @return chargeback fee
     */
    Money calculateChargebackFee(String providerName, Currency currency);

    /**
     * Get the fee structure for a PSP.
     *
     * @param providerName PSP provider name
     * @return fee structure details
     */
    FeeStructure getFeeStructure(String providerName);

    /**
     * Compare fees across multiple PSPs for a transaction.
     *
     * @param amount payment amount
     * @param paymentMethodType payment method
     * @param providerNames PSPs to compare
     * @return map of provider to calculated fee
     */
    Map<String, Money> compareFees(
            Money amount, 
            PaymentMethodType paymentMethodType,
            String... providerNames);

    /**
     * Get the cheapest PSP for a transaction.
     *
     * @param amount payment amount
     * @param paymentMethodType payment method
     * @param providerNames PSPs to compare
     * @return name of cheapest PSP
     */
    String getCheapestPsp(
            Money amount, 
            PaymentMethodType paymentMethodType,
            String... providerNames);

    /**
     * Fee structure for a PSP.
     */
    record FeeStructure(
            String providerName,
            BigDecimal percentageFee,
            Money fixedFee,
            Money minimumFee,
            Money maximumFee,
            Map<PaymentMethodType, BigDecimal> paymentMethodSurcharges,
            Map<Currency, BigDecimal> currencyRates,
            boolean chargesForRefunds,
            Money chargebackFee
    ) {
        /**
         * Calculate fee for a given amount.
         */
        public Money calculateFee(Money amount, PaymentMethodType paymentMethodType) {
            BigDecimal basePercentage = percentageFee;
            
            // Add payment method surcharge if applicable
            if (paymentMethodSurcharges != null && paymentMethodSurcharges.containsKey(paymentMethodType)) {
                basePercentage = basePercentage.add(paymentMethodSurcharges.get(paymentMethodType));
            }
            
            // Calculate percentage fee
            BigDecimal percentageFeeAmount = amount.getAmount()
                    .multiply(basePercentage)
                    .divide(BigDecimal.valueOf(100), amount.getAmount().scale(), java.math.RoundingMode.HALF_UP);
            
            // Add fixed fee
            BigDecimal totalFee = percentageFeeAmount;
            if (fixedFee != null && fixedFee.getCurrency() == amount.getCurrency()) {
                totalFee = totalFee.add(fixedFee.getAmount());
            }
            
            // Apply minimum fee
            if (minimumFee != null && minimumFee.getCurrency() == amount.getCurrency()) {
                if (totalFee.compareTo(minimumFee.getAmount()) < 0) {
                    totalFee = minimumFee.getAmount();
                }
            }
            
            // Apply maximum fee cap
            if (maximumFee != null && maximumFee.getCurrency() == amount.getCurrency()) {
                if (totalFee.compareTo(maximumFee.getAmount()) > 0) {
                    totalFee = maximumFee.getAmount();
                }
            }
            
            return new Money(totalFee, amount.getCurrency());
        }
    }
}
