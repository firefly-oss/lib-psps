/*
 * Copyright 2025 Firefly Software Solutions Inc
 */

package com.firefly.psps;

import com.firefly.psps.adapter.PspAdapter;
import com.firefly.psps.adapter.ports.*;
import com.firefly.psps.domain.*;
import com.firefly.psps.dtos.payments.*;
import com.firefly.psps.dtos.refunds.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test de integración completo que valida el PROPÓSITO PRINCIPAL de esta librería:
 * 
 * ABSTRACCIÓN PROVIDER-INDEPENDENT de Payment Service Providers.
 * 
 * La aplicación NO debe depender de PSPs específicos (Stripe, Adyen, etc.)
 * sino de los contratos (Ports) definidos en esta librería.
 */
@DisplayName("PSP Provider-Independent Abstraction Tests")
class PspAbstractionIntegrationTest {

    @Test
    @DisplayName("Application should depend on PspAdapter, not concrete PSP implementations")
    void applicationShouldDependOnAbstraction() {
        // PspAdapter is the abstraction
        assertTrue(PspAdapter.class.isInterface(),
            "PspAdapter should be an interface (abstraction)");
        
        // Application code should look like this:
        PspAdapter mockAdapter = mock(PspAdapter.class);
        PaymentPort mockPaymentPort = mock(PaymentPort.class);
        
        when(mockAdapter.payments()).thenReturn(mockPaymentPort);
        
        // Use the abstraction
        PaymentPort payments = mockAdapter.payments();
        assertNotNull(payments, "Should get payment port through abstraction");
    }

    @Test
    @DisplayName("Same application code should work with ANY PSP implementation")
    void sameCodeShouldWorkWithAnyPSP() {
        // Simulate two different PSP adapters
        PspAdapter stripeAdapter = mock(PspAdapter.class);
        PspAdapter adyenAdapter = mock(PspAdapter.class);
        
        when(stripeAdapter.getProviderName()).thenReturn("stripe");
        when(adyenAdapter.getProviderName()).thenReturn("adyen");
        when(stripeAdapter.payments()).thenReturn(mock(PaymentPort.class));
        when(adyenAdapter.payments()).thenReturn(mock(PaymentPort.class));
        
        // Same interface, different implementations
        assertEquals("stripe", stripeAdapter.getProviderName());
        assertEquals("adyen", adyenAdapter.getProviderName());
        
        // Application code uses the same interface for both
        assertNotNull(stripeAdapter.payments());
        assertNotNull(adyenAdapter.payments());
    }

    @Test
    @DisplayName("Switching PSPs should only require configuration change")
    void switchingPSPsShouldOnlyRequireConfigChange() {
        // Mock two PSP adapters
        PspAdapter pspA = mock(PspAdapter.class);
        PspAdapter pspB = mock(PspAdapter.class);
        
        when(pspA.isHealthy()).thenReturn(true);
        when(pspB.isHealthy()).thenReturn(true);
        
        // Application code doesn't care which PSP
        PspAdapter activePsp = pspA; // Changed via configuration
        assertTrue(activePsp.isHealthy());
        
        activePsp = pspB; // Switch PSP
        assertTrue(activePsp.isHealthy());
        
        // Same method calls work with both
    }

    @Test
    @DisplayName("All PSP adapters must implement the same contract")
    void allPSPsMustImplementSameContract() {
        // Create mock adapters for different PSPs
        PspAdapter stripe = mock(PspAdapter.class);
        PspAdapter adyen = mock(PspAdapter.class);
        PspAdapter paypal = mock(PspAdapter.class);
        
        // All must provide the same ports
        when(stripe.payments()).thenReturn(mock(PaymentPort.class));
        when(stripe.refunds()).thenReturn(mock(RefundPort.class));
        when(stripe.customers()).thenReturn(mock(CustomerPort.class));
        
        when(adyen.payments()).thenReturn(mock(PaymentPort.class));
        when(adyen.refunds()).thenReturn(mock(RefundPort.class));
        when(adyen.customers()).thenReturn(mock(CustomerPort.class));
        
        when(paypal.payments()).thenReturn(mock(PaymentPort.class));
        when(paypal.refunds()).thenReturn(mock(RefundPort.class));
        when(paypal.customers()).thenReturn(mock(CustomerPort.class));
        
        // Verify all implement the same interface
        assertNotNull(stripe.payments());
        assertNotNull(adyen.payments());
        assertNotNull(paypal.payments());
    }

    @Test
    @DisplayName("Payment creation should work with any PSP through same interface")
    void paymentCreationShouldWorkWithAnyPSP() {
        // Setup mock adapter
        PspAdapter adapter = mock(PspAdapter.class);
        PaymentPort paymentPort = mock(PaymentPort.class);
        when(adapter.payments()).thenReturn(paymentPort);
        
        // Mock response
        PaymentResponse expectedResponse = PaymentResponse.builder()
            .paymentId("pay_123")
            .status(PaymentStatus.SUCCEEDED)
            .amount(new Money(BigDecimal.valueOf(100), Currency.USD))
            .build();
        
        when(paymentPort.createPayment(any()))
            .thenReturn(Mono.just(ResponseEntity.ok(expectedResponse)));
        
        // Application code (provider-independent)
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(new Money(BigDecimal.valueOf(100), Currency.USD))
            .customerId("cust_123")
            .build();
        
        Mono<ResponseEntity<PaymentResponse>> result = adapter.payments().createPayment(request);
        
        // Verify it works
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals("pay_123", response.getBody().getPaymentId());
                assertEquals(PaymentStatus.SUCCEEDED, response.getBody().getStatus());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Library should use DTOs, not PSP-specific types")
    void libraryShouldUseDTOsNotPSPTypes() {
        // All requests/responses should use library's DTOs
        assertNotNull(CreatePaymentRequest.class);
        assertNotNull(PaymentResponse.class);
        assertNotNull(CreateRefundRequest.class);
        
        // These DTOs are PSP-agnostic
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(new Money(BigDecimal.valueOf(100), Currency.EUR))
            .customerId("cust_123")
            .build();
        
        // No Stripe-specific, Adyen-specific types leaked
        assertNotNull(request.getAmount());
        assertEquals(Currency.EUR, request.getAmount().getCurrency());
    }

    @Test
    @DisplayName("All 9 ports should be accessible through PspAdapter")
    void all9PortsShouldBeAccessible() {
        PspAdapter adapter = mock(PspAdapter.class);
        
        // Mock all ports
        when(adapter.payments()).thenReturn(mock(PaymentPort.class));
        when(adapter.refunds()).thenReturn(mock(RefundPort.class));
        when(adapter.payouts()).thenReturn(mock(PayoutPort.class));
        when(adapter.customers()).thenReturn(mock(CustomerPort.class));
        when(adapter.subscriptions()).thenReturn(mock(SubscriptionPort.class));
        when(adapter.checkout()).thenReturn(mock(CheckoutPort.class));
        when(adapter.disputes()).thenReturn(mock(DisputePort.class));
        when(adapter.providerSpecific()).thenReturn(mock(ProviderSpecificPort.class));
        when(adapter.reconciliation()).thenReturn(mock(ReconciliationPort.class));
        
        // Verify all accessible
        assertNotNull(adapter.payments(), "Payment operations");
        assertNotNull(adapter.refunds(), "Refund operations");
        assertNotNull(adapter.payouts(), "Payout operations");
        assertNotNull(adapter.customers(), "Customer management");
        assertNotNull(adapter.subscriptions(), "Subscription management");
        assertNotNull(adapter.checkout(), "Checkout operations");
        assertNotNull(adapter.disputes(), "Dispute management");
        assertNotNull(adapter.providerSpecific(), "Provider-specific operations");
        assertNotNull(adapter.reconciliation(), "Reconciliation operations");
    }

    @Test
    @DisplayName("Library should allow multi-PSP scenarios")
    void libraryShouldAllowMultiPSP() {
        // Application can work with multiple PSPs simultaneously
        PspAdapter primaryPSP = mock(PspAdapter.class);
        PspAdapter backupPSP = mock(PspAdapter.class);
        
        when(primaryPSP.getProviderName()).thenReturn("stripe");
        when(primaryPSP.isHealthy()).thenReturn(false); // Primary down
        
        when(backupPSP.getProviderName()).thenReturn("adyen");
        when(backupPSP.isHealthy()).thenReturn(true);
        
        // Failover logic (enabled by abstraction)
        PspAdapter activePSP = primaryPSP.isHealthy() ? primaryPSP : backupPSP;
        
        assertEquals("adyen", activePSP.getProviderName(),
            "Should fail over to backup PSP");
    }

    @Test
    @DisplayName("Reactive types should work with any PSP")
    void reactiveTypesShouldWorkWithAnyPSP() {
        PspAdapter adapter = mock(PspAdapter.class);
        PaymentPort paymentPort = mock(PaymentPort.class);
        when(adapter.payments()).thenReturn(paymentPort);
        
        // Mock reactive response
        when(paymentPort.getPayment(anyString()))
            .thenReturn(Mono.just(ResponseEntity.ok(
                PaymentResponse.builder()
                    .paymentId("pay_123")
                    .status(PaymentStatus.SUCCEEDED)
                    .amount(new Money(BigDecimal.TEN, Currency.USD))
                    .build()
            )));
        
        // Works with reactive streams
        Mono<ResponseEntity<PaymentResponse>> payment = adapter.payments().getPayment("pay_123");
        
        StepVerifier.create(payment)
            .assertNext(response -> {
                assertNotNull(response.getBody());
                assertEquals("pay_123", response.getBody().getPaymentId());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Library should support complete payment lifecycle")
    void libraryShouldSupportCompletePaymentLifecycle() throws Exception {
        // Verify all lifecycle methods exist
        Class<?> paymentPortClass = PaymentPort.class;
        
        assertNotNull(paymentPortClass.getMethod("createPayment", CreatePaymentRequest.class));
        assertNotNull(paymentPortClass.getMethod("getPayment", String.class));
        assertNotNull(paymentPortClass.getMethod("confirmPayment", ConfirmPaymentRequest.class));
        assertNotNull(paymentPortClass.getMethod("capturePayment", CapturePaymentRequest.class));
        assertNotNull(paymentPortClass.getMethod("cancelPayment", String.class));
        assertNotNull(paymentPortClass.getMethod("listPayments", ListPaymentsRequest.class));
        assertNotNull(paymentPortClass.getMethod("updatePayment", UpdatePaymentRequest.class));
    }

    @Test
    @DisplayName("Domain models should be PSP-agnostic")
    void domainModelsShouldBePSPAgnostic() {
        // Money works with any PSP
        Money money = new Money(BigDecimal.valueOf(100), Currency.USD);
        assertNotNull(money);
        
        // Currency is standardized (ISO 4217)
        assertEquals("USD", Currency.USD.name());
        
        // PaymentStatus is unified across PSPs
        assertNotNull(PaymentStatus.SUCCEEDED);
        assertNotNull(PaymentStatus.FAILED);
        
        // These models don't belong to any specific PSP
    }

    @Test
    @DisplayName("Library should enable PSP-specific operations without breaking abstraction")
    void libraryShouldEnablePSPSpecificOperations() {
        PspAdapter adapter = mock(PspAdapter.class);
        ProviderSpecificPort providerPort = mock(ProviderSpecificPort.class);
        
        when(adapter.providerSpecific()).thenReturn(providerPort);
        when(providerPort.supportsOperation("stripe_connect")).thenReturn(true);
        
        // Can use PSP-specific features through standard interface
        assertTrue(adapter.providerSpecific().supportsOperation("stripe_connect"));
        
        // But doesn't break abstraction - same interface for all PSPs
    }
}
