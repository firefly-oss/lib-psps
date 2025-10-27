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

import com.firefly.psps.domain.Money;
import com.firefly.psps.domain.PaymentStatus;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for payment reconciliation with PSP.
 * 
 * Reconciliation ensures consistency between:
 * - Your internal payment records
 * - PSP's transaction records
 * - Bank settlement reports
 * 
 * Critical for:
 * - Financial auditing
 * - Fraud detection
 * - Dispute resolution
 * - Regulatory compliance
 */
public interface ReconciliationPort {

    /**
     * Reconcile payments for a specific date.
     * 
     * Compares internal records with PSP data to find discrepancies.
     *
     * @param date date to reconcile
     * @return list of discrepancies found
     */
    Mono<List<PaymentDiscrepancy>> reconcilePayments(LocalDate date);

    /**
     * Reconcile payments within a date range.
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of discrepancies
     */
    Mono<List<PaymentDiscrepancy>> reconcilePayments(LocalDate startDate, LocalDate endDate);

    /**
     * Get settlement report from PSP for a date.
     *
     * @param date settlement date
     * @return settlement report
     */
    Mono<SettlementReport> getSettlementReport(LocalDate date);

    /**
     * Verify a specific payment state with PSP.
     * 
     * Fetches the payment from PSP and compares with provided state.
     *
     * @param paymentId internal payment ID
     * @param expectedStatus expected payment status
     * @param expectedAmount expected amount
     * @return discrepancy if found, empty if matching
     */
    Mono<PaymentDiscrepancy> verifyPaymentState(
            String paymentId, 
            PaymentStatus expectedStatus, 
            Money expectedAmount);

    /**
     * Get all transactions for a date from PSP.
     *
     * @param date transaction date
     * @return list of PSP transactions
     */
    Mono<List<PspTransaction>> getPspTransactions(LocalDate date);

    /**
     * Represents a discrepancy found during reconciliation.
     */
    record PaymentDiscrepancy(
            String paymentId,
            DiscrepancyType type,
            String description,
            Money internalAmount,
            Money pspAmount,
            PaymentStatus internalStatus,
            PaymentStatus pspStatus,
            LocalDate transactionDate
    ) {}

    /**
     * Types of reconciliation discrepancies.
     */
    enum DiscrepancyType {
        MISSING_IN_PSP,           // Payment in our DB but not in PSP
        MISSING_IN_INTERNAL,      // Payment in PSP but not in our DB
        AMOUNT_MISMATCH,          // Amounts don't match
        STATUS_MISMATCH,          // Payment states don't match
        CURRENCY_MISMATCH,        // Currency codes don't match
        DUPLICATE_TRANSACTION,    // Transaction appears multiple times
        REFUND_MISMATCH          // Refund amounts don't reconcile
    }

    /**
     * Settlement report from PSP.
     */
    record SettlementReport(
            LocalDate settlementDate,
            Money totalAmount,
            Money fees,
            Money netAmount,
            int transactionCount,
            List<PspTransaction> transactions
    ) {}

    /**
     * Transaction record from PSP.
     */
    record PspTransaction(
            String transactionId,
            String paymentId,
            Money amount,
            Money fee,
            PaymentStatus status,
            String type,
            LocalDate transactionDate
    ) {}
}
