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

package com.firefly.psps.compliance;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Compliance service for PCI-DSS, GDPR and regulatory requirements.
 * 
 * Ensures PSP operations comply with:
 * - PCI-DSS: No sensitive card data in logs/storage
 * - GDPR: Right to erasure, data portability
 * - AML: Anti-money laundering checks
 * - KYC: Know your customer requirements
 * 
 * NEVER log, store or transmit:
 * - Full credit card numbers (use tokens)
 * - CVV/CVC codes
 * - Unencrypted customer PII
 */
public interface ComplianceService {

    /**
     * Mask sensitive payment data for logging/display.
     * 
     * PCI-DSS compliant masking:
     * - Card: show only last 4 digits (****1234)
     * - IBAN: show only last 4 characters
     * - Email: partially mask (j***@example.com)
     *
     * @param data data to mask
     * @param dataType type of sensitive data
     * @return masked data safe for logging
     */
    String maskSensitiveData(String data, SensitiveDataType dataType);

    /**
     * Create an immutable audit log entry.
     * 
     * Audit logs must be:
     * - Immutable (append-only)
     * - Timestamped
     * - Include user/tenant context
     * - Retained per regulatory requirements
     *
     * @param event audit event to log
     * @return completion signal
     */
    Mono<Void> logAuditEvent(AuditEvent event);

    /**
     * Anonymize customer data for GDPR compliance.
     * 
     * Used for "right to erasure" requests.
     * Replaces PII with anonymized values while preserving data structure.
     *
     * @param customerId customer ID to anonymize
     * @return completion signal
     */
    Mono<Void> anonymizeCustomerData(String customerId);

    /**
     * Export customer data for GDPR data portability.
     * 
     * Returns all customer data in machine-readable format.
     *
     * @param customerId customer ID
     * @return customer data export
     */
    Mono<CustomerDataExport> exportCustomerData(String customerId);

    /**
     * Check if transaction requires enhanced due diligence.
     * 
     * Flags suspicious transactions for AML/fraud review:
     * - Large amounts
     * - Unusual patterns
     * - High-risk countries
     *
     * @param context transaction context
     * @return true if enhanced diligence required
     */
    boolean requiresEnhancedDueDiligence(TransactionContext context);

    /**
     * Validate if operation is PCI-DSS compliant.
     *
     * @param operation operation to validate
     * @return validation result
     */
    Mono<ComplianceValidationResult> validatePciCompliance(Map<String, Object> operation);

    /**
     * Types of sensitive data.
     */
    enum SensitiveDataType {
        CREDIT_CARD,
        CVV,
        IBAN,
        ACCOUNT_NUMBER,
        EMAIL,
        PHONE,
        SSN,
        TAX_ID,
        PASSPORT,
        API_KEY,
        PASSWORD
    }

    /**
     * Immutable audit event.
     */
    record AuditEvent(
            String eventId,
            String eventType,
            String userId,
            String tenantId,
            String resourceId,
            String action,
            Map<String, Object> metadata,
            Instant timestamp,
            String ipAddress,
            boolean successful,
            String errorMessage
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String eventId;
            private String eventType;
            private String userId;
            private String tenantId;
            private String resourceId;
            private String action;
            private Map<String, Object> metadata = Map.of();
            private Instant timestamp = Instant.now();
            private String ipAddress;
            private boolean successful = true;
            private String errorMessage;

            public Builder eventId(String eventId) {
                this.eventId = eventId;
                return this;
            }

            public Builder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }

            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder tenantId(String tenantId) {
                this.tenantId = tenantId;
                return this;
            }

            public Builder resourceId(String resourceId) {
                this.resourceId = resourceId;
                return this;
            }

            public Builder action(String action) {
                this.action = action;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder ipAddress(String ipAddress) {
                this.ipAddress = ipAddress;
                return this;
            }

            public Builder successful(boolean successful) {
                this.successful = successful;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public AuditEvent build() {
                return new AuditEvent(
                        eventId, eventType, userId, tenantId, resourceId,
                        action, metadata, timestamp, ipAddress, successful, errorMessage
                );
            }
        }
    }

    /**
     * Customer data export for GDPR.
     */
    record CustomerDataExport(
            String customerId,
            Map<String, Object> personalData,
            java.util.List<Map<String, Object>> transactions,
            java.util.List<Map<String, Object>> paymentMethods,
            Instant exportedAt
    ) {}

    /**
     * Transaction context for compliance checks.
     */
    record TransactionContext(
            String customerId,
            com.firefly.psps.domain.Money amount,
            com.firefly.psps.domain.Currency currency,
            String country,
            String ipAddress,
            boolean recurringPayment
    ) {}

    /**
     * Compliance validation result.
     */
    record ComplianceValidationResult(
            boolean isCompliant,
            java.util.List<String> violations,
            java.util.List<String> warnings
    ) {
        public static ComplianceValidationResult compliant() {
            return new ComplianceValidationResult(true, java.util.List.of(), java.util.List.of());
        }

        public static ComplianceValidationResult nonCompliant(java.util.List<String> violations) {
            return new ComplianceValidationResult(false, violations, java.util.List.of());
        }
    }
}
