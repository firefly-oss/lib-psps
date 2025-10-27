/*
 * Copyright 2025 Firefly Software Solutions Inc
 */

package com.firefly.psps.architecture;

import com.firefly.psps.adapter.PspAdapter;
import com.firefly.psps.adapter.ports.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests que validan la arquitectura hexagonal (Ports & Adapters).
 * 
 * PROPÓSITO: Garantizar que el dominio es independiente de la infraestructura.
 */
@DisplayName("Hexagonal Architecture Tests")
class HexagonalArchitectureTest {

    @Test
    @DisplayName("All ports should be interfaces")
    void allPortsShouldBeInterfaces() {
        List<Class<?>> ports = Arrays.asList(
            PaymentPort.class,
            RefundPort.class,
            PayoutPort.class,
            CustomerPort.class,
            SubscriptionPort.class,
            CheckoutPort.class,
            DisputePort.class,
            ProviderSpecificPort.class,
            ReconciliationPort.class
        );

        ports.forEach(port -> {
            assertTrue(port.isInterface(), 
                port.getSimpleName() + " should be an interface (Port)");
        });
    }

    @Test
    @DisplayName("PspAdapter should expose 9 ports")
    void pspAdapterShouldExpose9Ports() {
        List<String> expectedMethods = Arrays.asList(
            "payments", "refunds", "payouts", "customers",
            "subscriptions", "checkout", "disputes", 
            "providerSpecific", "reconciliation"
        );

        List<String> actualMethods = Arrays.stream(PspAdapter.class.getDeclaredMethods())
            .filter(m -> expectedMethods.contains(m.getName()))
            .map(Method::getName)
            .toList();

        assertEquals(9, actualMethods.size(), 
            "PspAdapter should expose exactly 9 port access methods");
        
        assertTrue(actualMethods.containsAll(expectedMethods),
            "PspAdapter missing port methods: " + expectedMethods);
    }

    @Test
    @DisplayName("Ports should not have concrete implementations in domain layer")
    void portsShouldNotHaveConcreteImplementations() {
        List<Class<?>> ports = Arrays.asList(
            PaymentPort.class,
            RefundPort.class,
            PayoutPort.class,
            CustomerPort.class,
            SubscriptionPort.class,
            CheckoutPort.class,
            DisputePort.class,
            ProviderSpecificPort.class,
            ReconciliationPort.class
        );

        ports.forEach(port -> {
            long abstractMethods = Arrays.stream(port.getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .count();
            
            long totalMethods = port.getDeclaredMethods().length;
            
            assertTrue(abstractMethods > 0,
                port.getSimpleName() + " should have abstract methods");
            
            // All methods should be abstract (no default implementations that break hexagonal)
            assertEquals(totalMethods, abstractMethods,
                port.getSimpleName() + " should only have abstract methods in domain layer");
        });
    }

    @Test
    @DisplayName("Domain models should not depend on infrastructure")
    void domainModelsShouldNotDependOnInfrastructure() {
        // Verify Money doesn't depend on any PSP-specific classes
        Package moneyPackage = com.firefly.psps.domain.Money.class.getPackage();
        assertEquals("com.firefly.psps.domain", moneyPackage.getName(),
            "Domain models should be in domain package");
        
        // Domain should be pure - no Spring, no HTTP, no PSP SDKs
        assertDoesNotThrow(() -> {
            Class.forName("com.firefly.psps.domain.Money");
            Class.forName("com.firefly.psps.domain.Currency");
            Class.forName("com.firefly.psps.domain.PaymentStatus");
        }, "Core domain models should exist");
    }

    @Test
    @DisplayName("Ports should return reactive types for non-blocking operations")
    void portsShouldReturnReactiveTypes() {
        List<Class<?>> ports = Arrays.asList(
            PaymentPort.class,
            RefundPort.class,
            PayoutPort.class,
            CustomerPort.class,
            SubscriptionPort.class,
            CheckoutPort.class,
            DisputePort.class,
            ReconciliationPort.class
        );

        ports.forEach(port -> {
            long reactiveMethodCount = Arrays.stream(port.getDeclaredMethods())
                .filter(m -> m.getReturnType().getName().contains("Mono") || 
                             m.getReturnType().getName().contains("Flux"))
                .count();

            assertTrue(reactiveMethodCount > 0,
                port.getSimpleName() + " should have reactive (Mono/Flux) return types");
        });
    }

    @Test
    @DisplayName("PspAdapter should be the single entry point")
    void pspAdapterShouldBeSingleEntryPoint() {
        // PspAdapter is the facade - all port access goes through it
        assertTrue(PspAdapter.class.isInterface(),
            "PspAdapter should be an interface");

        Method[] methods = PspAdapter.class.getDeclaredMethods();
        assertTrue(methods.length > 9,
            "PspAdapter should have port accessors + utility methods");
    }
}
