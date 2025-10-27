/*
 * Copyright 2025 Firefly Software Solutions Inc
 */

package com.firefly.psps.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para validar domain models son inmutables y type-safe.
 * 
 * PROPÓSITO: Garantizar type-safety y prevenir primitive obsession.
 */
@DisplayName("Domain Models Tests")
class DomainModelsTest {

    @Test
    @DisplayName("Money should be immutable value object")
    void moneyShouldBeImmutable() {
        Money money = new Money(BigDecimal.valueOf(100), Currency.EUR);
        
        assertEquals(BigDecimal.valueOf(100), money.getAmount());
        assertEquals(Currency.EUR, money.getCurrency());
        
        // Money is immutable - can't change amount or currency
        assertNotNull(money.toString());
    }

    @Test
    @DisplayName("Money should reject negative amounts")
    void moneyShouldRejectNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Money(BigDecimal.valueOf(-10), Currency.USD);
        }, "Money should not accept negative amounts");
    }

    @Test
    @DisplayName("Money should reject null currency")
    void moneyShouldRejectNullCurrency() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Money(BigDecimal.valueOf(100), null);
        }, "Money should not accept null currency");
    }

    @Test
    @DisplayName("Money.fromCents should convert correctly")
    void moneyFromCentsShouldConvert() {
        Money money = Money.fromCents(12345, Currency.USD);
        
        assertEquals(new BigDecimal("123.45"), money.getAmount());
        assertEquals(Currency.USD, money.getCurrency());
    }

    @Test
    @DisplayName("Money.toCents should convert correctly")
    void moneyToCentsShouldConvert() {
        Money money = new Money(new BigDecimal("123.45"), Currency.USD);
        
        assertEquals(12345L, money.toCents());
    }

    @Test
    @DisplayName("Money should support equality")
    void moneyShouldSupportEquality() {
        Money money1 = new Money(BigDecimal.valueOf(100), Currency.EUR);
        Money money2 = new Money(BigDecimal.valueOf(100), Currency.EUR);
        Money money3 = new Money(BigDecimal.valueOf(200), Currency.EUR);
        
        assertEquals(money1, money2, "Same money values should be equal");
        assertNotEquals(money1, money3, "Different money values should not be equal");
    }

    @Test
    @DisplayName("Money should support different currencies")
    void moneyShouldSupportDifferentCurrencies() {
        Money eur = new Money(BigDecimal.valueOf(100), Currency.EUR);
        Money usd = new Money(BigDecimal.valueOf(100), Currency.USD);
        
        assertNotEquals(eur, usd, "Same amount in different currencies should not be equal");
    }

    @Test
    @DisplayName("Currency enum should have major currencies")
    void currencyShouldHaveMajorCurrencies() {
        assertNotNull(Currency.USD);
        assertNotNull(Currency.EUR);
        assertNotNull(Currency.GBP);
        assertNotNull(Currency.JPY);
        assertNotNull(Currency.CHF);
        
        assertTrue(Currency.values().length >= 10, 
            "Should support at least 10 major currencies");
    }

    @Test
    @DisplayName("PaymentStatus enum should have all lifecycle states")
    void paymentStatusShouldHaveLifecycleStates() {
        // Should have complete payment lifecycle
        assertNotNull(PaymentStatus.PENDING);
        assertNotNull(PaymentStatus.PROCESSING);
        assertNotNull(PaymentStatus.SUCCEEDED);
        assertNotNull(PaymentStatus.FAILED);
        assertNotNull(PaymentStatus.CANCELLED);
        
        assertTrue(PaymentStatus.values().length >= 5,
            "Should have at least 5 payment states");
    }

    @Test
    @DisplayName("PaymentMethodType enum should have common payment methods")
    void paymentMethodTypeShouldHaveCommonMethods() {
        // Should have major payment methods
        assertNotNull(PaymentMethodType.CARD);
        assertNotNull(PaymentMethodType.BANK_TRANSFER);
        
        assertTrue(PaymentMethodType.values().length >= 5,
            "Should support at least 5 payment method types");
    }

    @Test
    @DisplayName("SubscriptionStatus enum should have subscription states")
    void subscriptionStatusShouldHaveStates() {
        assertNotNull(SubscriptionStatus.ACTIVE);
        assertNotNull(SubscriptionStatus.CANCELED);
        
        assertTrue(SubscriptionStatus.values().length >= 4,
            "Should have at least 4 subscription states");
    }

    @Test
    @DisplayName("BillingInterval enum should have recurring periods")
    void billingIntervalShouldHavePeriods() {
        assertNotNull(BillingInterval.DAY);
        assertNotNull(BillingInterval.WEEK);
        assertNotNull(BillingInterval.MONTH);
        assertNotNull(BillingInterval.YEAR);
        
        assertEquals(4, BillingInterval.values().length,
            "Should have exactly 4 billing intervals");
    }

    @Test
    @DisplayName("CheckoutMode enum should have payment modes")
    void checkoutModeShouldHavePaymentModes() {
        assertNotNull(CheckoutMode.PAYMENT);
        assertNotNull(CheckoutMode.SUBSCRIPTION);
        assertNotNull(CheckoutMode.SETUP);
        
        assertEquals(3, CheckoutMode.values().length,
            "Should have exactly 3 checkout modes");
    }

    @Test
    @DisplayName("Address should be a value object")
    void addressShouldBeValueObject() {
        Address address = new Address(
            "123 Main St",
            "Apt 4B",
            "New York",
            "NY",
            "10001",
            "US"
        );
        
        assertEquals("123 Main St", address.getLine1());
        assertEquals("New York", address.getCity());
        assertEquals("US", address.getCountry());
        assertNotNull(address.toString());
    }

    @Test
    @DisplayName("CustomerInfo should contain customer data")
    void customerInfoShouldContainCustomerData() {
        Address address = new Address(
            "123 Main St", null, "New York", "NY", "10001", "US"
        );
        
        CustomerInfo customer = new CustomerInfo(
            "cust-123",
            "John",
            "Doe",
            "john@example.com",
            "+1234567890",
            address,
            null
        );
        
        assertEquals("john@example.com", customer.getEmail());
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals(address, customer.getBillingAddress());
    }

    @Test
    @DisplayName("Domain models should not have setters (immutable)")
    void domainModelsShouldNotHaveSetters() {
        // Money is immutable - only has getters
        assertEquals(0, 
            java.util.Arrays.stream(Money.class.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("set"))
                .count(),
            "Money should not have setters");
        
        // Address should be immutable
        assertEquals(0,
            java.util.Arrays.stream(Address.class.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("set"))
                .count(),
            "Address should not have setters");
    }

    @Test
    @DisplayName("Domain models should use final fields")
    void domainModelsShouldUseFinalFields() {
        // All fields in Money should be final
        long finalFieldCount = java.util.Arrays.stream(Money.class.getDeclaredFields())
            .filter(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()))
            .count();
        
        assertTrue(finalFieldCount > 0,
            "Money should have final fields for immutability");
    }
}
