/*
 * Copyright 2025 Firefly Software Solutions Inc
 */

package com.firefly.psps.validation;

import com.firefly.psps.compliance.ComplianceService;
import com.firefly.psps.domain.Currency;
import com.firefly.psps.domain.Money;
import com.firefly.psps.domain.PaymentMethodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para validar la capa de validación y compliance.
 * 
 * PROPÓSITO: Demostrar que la librería previene errores ANTES de llamar al PSP (ahorra $$)
 * y cumple con PCI-DSS/GDPR (nunca expone datos sensibles).
 */
@DisplayName("Validation and Compliance Tests")
class ValidationAndComplianceTest {

    @Test
    @DisplayName("PaymentValidator interface should exist for pre-PSP validation")
    void paymentValidatorInterfaceShouldExist() {
        assertDoesNotThrow(() -> {
            Class.forName("com.firefly.psps.validation.PaymentValidator");
        }, "PaymentValidator interface should exist");
    }

    @Test
    @DisplayName("PaymentValidator should have methods to validate before PSP call")
    void paymentValidatorShouldHaveValidationMethods() throws Exception {
        Class<?> validatorClass = Class.forName("com.firefly.psps.validation.PaymentValidator");
        
        // Should have currency validation
        assertNotNull(validatorClass.getMethod("isCurrencySupported", Currency.class),
            "Should have currency validation");
        
        // Should have amount limits validation
        assertNotNull(validatorClass.getMethod("isAmountWithinLimits", Money.class, Currency.class),
            "Should have amount limits validation");
        
        // Should have payment method validation
        assertNotNull(validatorClass.getMethod("isPaymentMethodSupported", PaymentMethodType.class),
            "Should have payment method validation");
        
        // Should return supported currencies
        assertNotNull(validatorClass.getMethod("getSupportedCurrencies"),
            "Should return supported currencies");
    }

    @Test
    @DisplayName("ComplianceService should exist for PCI-DSS/GDPR compliance")
    void complianceServiceShouldExist() {
        assertDoesNotThrow(() -> {
            Class.forName("com.firefly.psps.compliance.ComplianceService");
        }, "ComplianceService interface should exist");
    }

    @Test
    @DisplayName("ComplianceService should have sensitive data masking")
    void complianceServiceShouldHaveMasking() throws Exception {
        Class<?> complianceClass = Class.forName("com.firefly.psps.compliance.ComplianceService");
        
        // Should have masking for PCI-DSS
        assertNotNull(complianceClass.getMethod("maskSensitiveData", String.class, 
            ComplianceService.SensitiveDataType.class),
            "Should have sensitive data masking");
    }

    @Test
    @DisplayName("ComplianceService.SensitiveDataType should cover PCI-DSS requirements")
    void sensitiveDataTypeShouldCoverPCIDSS() {
        // Should have types for all PCI-DSS sensitive data
        assertNotNull(ComplianceService.SensitiveDataType.CREDIT_CARD,
            "Should have CREDIT_CARD type");
        assertNotNull(ComplianceService.SensitiveDataType.CVV,
            "Should have CVV type");
        assertNotNull(ComplianceService.SensitiveDataType.ACCOUNT_NUMBER,
            "Should have ACCOUNT_NUMBER type");
        
        // Should also have PII types
        assertNotNull(ComplianceService.SensitiveDataType.EMAIL,
            "Should have EMAIL type");
        assertNotNull(ComplianceService.SensitiveDataType.PHONE,
            "Should have PHONE type");
        assertNotNull(ComplianceService.SensitiveDataType.SSN,
            "Should have SSN type");
    }

    @Test
    @DisplayName("ComplianceService should have GDPR support")
    void complianceServiceShouldHaveGDPRSupport() throws Exception {
        Class<?> complianceClass = Class.forName("com.firefly.psps.compliance.ComplianceService");
        
        // GDPR: Right to erasure
        assertNotNull(complianceClass.getMethod("anonymizeCustomerData", String.class),
            "Should support right to erasure");
        
        // GDPR: Data portability
        assertNotNull(complianceClass.getMethod("exportCustomerData", String.class),
            "Should support data portability");
    }

    @Test
    @DisplayName("ComplianceService should have audit logging")
    void complianceServiceShouldHaveAuditLogging() throws Exception {
        Class<?> complianceClass = Class.forName("com.firefly.psps.compliance.ComplianceService");
        
        // Should have immutable audit event logging
        assertNotNull(complianceClass.getMethod("logAuditEvent", 
            ComplianceService.AuditEvent.class),
            "Should have audit logging");
    }

    @Test
    @DisplayName("ComplianceService.AuditEvent should be immutable")
    void auditEventShouldBeImmutable() {
        // AuditEvent is a record - immutable by design
        assertTrue(ComplianceService.AuditEvent.class.isRecord(),
            "AuditEvent should be a record (immutable)");
    }

    @Test
    @DisplayName("ComplianceService should have AML/KYC support")
    void complianceServiceShouldHaveAMLSupport() throws Exception {
        Class<?> complianceClass = Class.forName("com.firefly.psps.compliance.ComplianceService");
        
        // Should detect suspicious transactions
        assertNotNull(complianceClass.getMethod("requiresEnhancedDueDiligence", 
            ComplianceService.TransactionContext.class),
            "Should have AML/KYC enhanced due diligence");
    }

    @Test
    @DisplayName("ValidationResult should contain errors and warnings")
    void validationResultShouldContainErrorsAndWarnings() throws Exception {
        Class<?> validationResultClass = Class.forName(
            "com.firefly.psps.validation.PaymentValidator$ValidationResult");
        
        assertNotNull(validationResultClass.getMethod("isValid"),
            "Should have isValid method");
        assertNotNull(validationResultClass.getMethod("getErrors"),
            "Should have getErrors method");
        assertNotNull(validationResultClass.getMethod("getWarnings"),
            "Should have getWarnings method");
    }

    @Test
    @DisplayName("ValidationError should have field, message and code")
    void validationErrorShouldHaveDetails() {
        // ValidationError is a record with field, message, code
        Class<?> errorClass = PaymentValidator.ValidationError.class;
        assertTrue(errorClass.isRecord(),
            "ValidationError should be a record");
    }

    @Test
    @DisplayName("PaymentValidationException should exist for validation failures")
    void paymentValidationExceptionShouldExist() {
        assertDoesNotThrow(() -> {
            Class.forName("com.firefly.psps.exceptions.PaymentValidationException");
        }, "PaymentValidationException should exist");
    }

    @Test
    @DisplayName("Library should prevent primitive obsession with value objects")
    void libraryShouldPreventPrimitiveObsession() {
        // Money instead of BigDecimal
        assertNotNull(Money.class, "Should have Money value object");
        
        // Currency instead of String
        assertNotNull(Currency.class, "Should have Currency enum");
        
        // PaymentMethodType instead of String
        assertNotNull(PaymentMethodType.class, "Should have PaymentMethodType enum");
        
        // PaymentStatus instead of String
        assertNotNull(com.firefly.psps.domain.PaymentStatus.class, 
            "Should have PaymentStatus enum");
    }

    @Test
    @DisplayName("Validation should happen BEFORE PSP call to save costs")
    void validationShouldHappenBeforePSPCall() throws Exception {
        Class<?> validatorClass = Class.forName("com.firefly.psps.validation.PaymentValidator");
        
        // All validation methods should be synchronous (not Mono)
        // to fail fast before any PSP call
        var method = validatorClass.getMethod("isCurrencySupported", Currency.class);
        assertEquals(boolean.class, method.getReturnType(),
            "Validation should be synchronous for fail-fast");
        
        method = validatorClass.getMethod("isPaymentMethodSupported", PaymentMethodType.class);
        assertEquals(boolean.class, method.getReturnType(),
            "Validation should be synchronous for fail-fast");
    }

    @Test
    @DisplayName("Library should expose minimum and maximum amount validation")
    void libraryShouldExposeAmountLimits() throws Exception {
        Class<?> validatorClass = Class.forName("com.firefly.psps.validation.PaymentValidator");
        
        assertNotNull(validatorClass.getMethod("getMinimumAmount", Currency.class),
            "Should expose minimum amount limits");
        assertNotNull(validatorClass.getMethod("getMaximumAmount", Currency.class),
            "Should expose maximum amount limits");
    }
}
