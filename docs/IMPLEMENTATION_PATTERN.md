# PSP Implementation Pattern

## Overview

This document explains how microservices should implement PSP integrations using the `lib-psps` library. The library provides a complete abstraction layer that requires minimal code from implementations.

---

## Architecture Pattern

```
┌─────────────────────────────────────────────────────────────┐
│          YOUR MICROSERVICE (e.g., lib-psps-stripe-impl)    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. EXTEND AbstractPspService (1 class, ~3 lines)          │
│     └─ Inherits ALL service methods (20+ operations)        │
│                                                              │
│  2. EXTEND Abstract Controllers (5 classes, ~15 lines)      │
│     ├─ StripePaymentController extends AbstractPaymentController
│     ├─ StripeRefundController extends AbstractRefundController
│     ├─ StripeCheckoutController extends AbstractCheckoutController
│     ├─ StripeCustomerController extends AbstractCustomerController
│     └─ StripeSubscriptionController extends AbstractSubscriptionController
│                                                              │
│  3. IMPLEMENT Port Interfaces (9 classes, ~500-1000 lines)  │
│     ├─ StripePaymentPort implements PaymentPort             │
│     ├─ StripeRefundPort implements RefundPort               │
│     ├─ StripePayoutPort implements PayoutPort               │
│     ├─ StripeCustomerPort implements CustomerPort           │
│     ├─ StripeSubscriptionPort implements SubscriptionPort   │
│     ├─ StripeCheckoutPort implements CheckoutPort           │
│     ├─ StripeDisputePort implements DisputePort             │
│     ├─ StripeProviderSpecificPort extends AbstractProviderSpecificPort
│     └─ StripeReconciliationPort implements ReconciliationPort
│                                                              │
│  4. CREATE Mappers (convert between lib-psps DTOs and       │
│                      provider-specific DTOs)                 │
│                                                              │
│  5. CONFIGURE Spring Boot Auto-Configuration                │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ Uses
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    lib-psps (CORE LIBRARY)                  │
├─────────────────────────────────────────────────────────────┤
│  • 9 Port Interfaces (62+ methods)                          │
│  • 5 Abstract Controllers (22 REST endpoints)                │
│  • 1 Abstract Service (20+ methods with logging)            │
│  • Domain Models (9 immutable value objects)                │
│  • 41 DTOs (Request/Response)                               │
│  • Enterprise Features (Resilience, Observability, etc.)    │
└─────────────────────────────────────────────────────────────┘
```

---

## What You Get For FREE

By extending the abstract components, your implementation automatically gets:

### ✅ Complete REST API (22 endpoints)
- 5 Payment endpoints
- 3 Subscription endpoints
- 2 Refund endpoints
- 5 Checkout endpoints
- 7 Customer endpoints

### ✅ Service Layer with:
- Structured logging (SLF4J)
- Error handling and exception mapping
- Request/response validation
- Reactive programming (Project Reactor)

### ✅ Enterprise Features:
- Circuit breaker (prevents cascading failures)
- Rate limiting (protects PSP quotas)
- Retry with exponential backoff
- Bulkhead (limits concurrent calls)
- Timeout protection
- Metrics (Prometheus/Micrometer)
- Health checks (Spring Actuator)
- PCI-DSS compliance (data masking)
- GDPR support (anonymization, export)

### ✅ Configuration:
- Spring Boot auto-configuration
- Property-based configuration
- Conditional bean creation
- Multi-tenancy support

---

## Implementation Steps

### Step 1: Extend AbstractPspService

Create ONE service class that extends `AbstractPspService`:

```java
package com.firefly.psps.stripe.services;

import com.firefly.psps.adapter.PspAdapter;
import com.firefly.psps.services.AbstractPspService;
import org.springframework.stereotype.Service;

@Service
public class StripePspService extends AbstractPspService {
    
    public StripePspService(PspAdapter pspAdapter) {
        super(pspAdapter);
    }
    
    // That's it! All methods inherited:
    // - createPayment, getPayment, confirmPayment, capturePayment, cancelPayment
    // - createRefund, getRefund
    // - createSubscription, getSubscription, cancelSubscription
    // - createCheckoutSession, getCheckoutSession
    // - createPaymentIntent, getPaymentIntent, updatePaymentIntent
    // - createCustomer, getCustomer, updateCustomer, deleteCustomer
    // - attachPaymentMethod, listPaymentMethods, detachPaymentMethod
    
    // Optionally override methods for custom behavior:
    // @Override
    // public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
    //     // Custom pre-processing
    //     return super.createPayment(request)
    //         .doOnSuccess(response -> {
    //             // Custom post-processing
    //         });
    // }
}
```

**Lines of code:** ~5 lines  
**Methods inherited:** 20+ operations

---

### Step 2: Extend Abstract Controllers

Create 5 controller classes (one for each domain):

#### 2.1 Payment Controller

```java
package com.firefly.psps.stripe.controllers;

import com.firefly.psps.controllers.AbstractPaymentController;
import com.firefly.psps.services.AbstractPspService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${firefly.psp.base-path:/api/psp}/payments")
public class StripePaymentController extends AbstractPaymentController {
    
    public StripePaymentController(AbstractPspService pspService) {
        super(pspService);
    }
    
    // Inherits 5 endpoints:
    // POST   /api/psp/payments
    // GET    /api/psp/payments/{id}
    // POST   /api/psp/payments/{id}/confirm
    // POST   /api/psp/payments/{id}/capture
    // POST   /api/psp/payments/{id}/cancel
}
```

#### 2.2 Refund Controller

```java
@RestController
@RequestMapping("${firefly.psp.base-path:/api/psp}/refunds")
public class StripeRefundController extends AbstractRefundController {
    
    public StripeRefundController(AbstractPspService pspService) {
        super(pspService);
    }
    
    // Inherits 2 endpoints:
    // POST   /api/psp/refunds
    // GET    /api/psp/refunds/{id}
}
```

#### 2.3 Checkout Controller

```java
@RestController
@RequestMapping("${firefly.psp.base-path:/api/psp}/checkout")
public class StripeCheckoutController extends AbstractCheckoutController {
    
    public StripeCheckoutController(AbstractPspService pspService) {
        super(pspService);
    }
    
    // Inherits 5 endpoints:
    // POST   /api/psp/checkout/sessions
    // GET    /api/psp/checkout/sessions/{id}
    // POST   /api/psp/checkout/payment-intents
    // GET    /api/psp/checkout/payment-intents/{id}
    // PATCH  /api/psp/checkout/payment-intents/{id}
}
```

#### 2.4 Customer Controller

```java
@RestController
@RequestMapping("${firefly.psp.base-path:/api/psp}/customers")
public class StripeCustomerController extends AbstractCustomerController {
    
    public StripeCustomerController(AbstractPspService pspService) {
        super(pspService);
    }
    
    // Inherits 7 endpoints:
    // POST   /api/psp/customers
    // GET    /api/psp/customers/{id}
    // PATCH  /api/psp/customers/{id}
    // DELETE /api/psp/customers/{id}
    // POST   /api/psp/customers/{id}/payment-methods
    // GET    /api/psp/customers/{id}/payment-methods
    // DELETE /api/psp/customers/{id}/payment-methods/{pmId}
}
```

#### 2.5 Subscription Controller

```java
@RestController
@RequestMapping("${firefly.psp.base-path:/api/psp}/subscriptions")
public class StripeSubscriptionController extends AbstractSubscriptionController {
    
    public StripeSubscriptionController(AbstractPspService pspService) {
        super(pspService);
    }
    
    // Inherits 3 endpoints:
    // POST   /api/psp/subscriptions
    // GET    /api/psp/subscriptions/{id}
    // POST   /api/psp/subscriptions/{id}/cancel
}
```

**Lines of code per controller:** ~3 lines  
**Total:** ~15 lines for all 5 controllers  
**Endpoints inherited:** 22 REST endpoints

---

### Step 3: Implement Port Interfaces

This is where you write the PSP-specific logic. You need to implement 9 port interfaces:

#### 3.1 Payment Port Example

```java
package com.firefly.psps.stripe.adapters;

import com.firefly.psps.adapter.ports.PaymentPort;
import com.firefly.psps.dtos.payments.*;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class StripePaymentPort implements PaymentPort {
    
    private final StripeClient stripeClient;
    private final PaymentMapper mapper;
    
    public StripePaymentPort(StripeClient stripeClient, PaymentMapper mapper) {
        this.stripeClient = stripeClient;
        this.mapper = mapper;
    }
    
    @Override
    public Mono<ResponseEntity<PaymentResponse>> createPayment(CreatePaymentRequest request) {
        return Mono.fromCallable(() -> {
            // 1. Convert lib-psps DTO to Stripe DTO
            var stripeParams = mapper.toStripePaymentIntentParams(request);
            
            // 2. Call Stripe API
            PaymentIntent stripePayment = PaymentIntent.create(stripeParams);
            
            // 3. Convert Stripe response to lib-psps DTO
            PaymentResponse response = mapper.toPaymentResponse(stripePayment);
            
            return ResponseEntity.ok(response);
        });
    }
    
    @Override
    public Mono<ResponseEntity<PaymentResponse>> getPayment(String paymentId) {
        return Mono.fromCallable(() -> {
            PaymentIntent stripePayment = PaymentIntent.retrieve(paymentId);
            return ResponseEntity.ok(mapper.toPaymentResponse(stripePayment));
        });
    }
    
    @Override
    public Mono<ResponseEntity<PaymentResponse>> confirmPayment(ConfirmPaymentRequest request) {
        return Mono.fromCallable(() -> {
            PaymentIntent stripePayment = PaymentIntent.retrieve(request.getPaymentId());
            var params = mapper.toStripeConfirmParams(request);
            stripePayment = stripePayment.confirm(params);
            return ResponseEntity.ok(mapper.toPaymentResponse(stripePayment));
        });
    }
    
    // Implement remaining 4 methods: capturePayment, cancelPayment, listPayments, updatePayment
}
```

**You need to implement:**
- ✅ PaymentPort (7 methods)
- ✅ RefundPort (5 methods)
- ✅ PayoutPort (5 methods)
- ✅ CustomerPort (9 methods)
- ✅ SubscriptionPort (16 methods)
- ✅ CheckoutPort (7 methods)
- ✅ DisputePort (5 methods)
- ✅ ProviderSpecificPort (4+ methods)
- ✅ ReconciliationPort (5 methods)

**Lines of code:** ~500-1000 lines total (mostly simple mapping logic)

---

### Step 4: Create Mappers

Create mapper classes to convert between lib-psps DTOs and provider-specific DTOs:

```java
package com.firefly.psps.stripe.mappers;

import com.firefly.psps.domain.*;
import com.firefly.psps.dtos.payments.*;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

public class PaymentMapper {
    
    public static PaymentIntentCreateParams toStripePaymentIntentParams(CreatePaymentRequest request) {
        return PaymentIntentCreateParams.builder()
            .setAmount(request.getAmount().toCents())
            .setCurrency(request.getAmount().getCurrency().name().toLowerCase())
            .setCustomer(request.getCustomerId())
            .setDescription(request.getDescription())
            .build();
    }
    
    public static PaymentResponse toPaymentResponse(PaymentIntent stripePayment) {
        return PaymentResponse.builder()
            .paymentId(stripePayment.getId())
            .amount(Money.fromCents(stripePayment.getAmount(), 
                Currency.valueOf(stripePayment.getCurrency().toUpperCase())))
            .status(mapStatus(stripePayment.getStatus()))
            .customerId(stripePayment.getCustomer())
            .providerPaymentId(stripePayment.getId())
            .createdAt(Instant.ofEpochSecond(stripePayment.getCreated()))
            .build();
    }
    
    private static PaymentStatus mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_action" -> PaymentStatus.REQUIRES_ACTION;
            case "canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
    }
}
```

---

### Step 5: Implement PspAdapter

Create the main adapter that ties everything together:

```java
package com.firefly.psps.stripe.adapters;

import com.firefly.psps.adapter.PspAdapter;
import com.firefly.psps.adapter.ports.*;
import org.springframework.stereotype.Component;

@Component
public class StripePspAdapter implements PspAdapter {
    
    private final StripePaymentPort paymentPort;
    private final StripeRefundPort refundPort;
    private final StripePayoutPort payoutPort;
    private final StripeCustomerPort customerPort;
    private final StripeSubscriptionPort subscriptionPort;
    private final StripeCheckoutPort checkoutPort;
    private final StripeDisputePort disputePort;
    private final StripeProviderSpecificPort providerSpecificPort;
    private final StripeReconciliationPort reconciliationPort;
    
    public StripePspAdapter(
            StripePaymentPort paymentPort,
            StripeRefundPort refundPort,
            StripePayoutPort payoutPort,
            StripeCustomerPort customerPort,
            StripeSubscriptionPort subscriptionPort,
            StripeCheckoutPort checkoutPort,
            StripeDisputePort disputePort,
            StripeProviderSpecificPort providerSpecificPort,
            StripeReconciliationPort reconciliationPort) {
        this.paymentPort = paymentPort;
        this.refundPort = refundPort;
        this.payoutPort = payoutPort;
        this.customerPort = customerPort;
        this.subscriptionPort = subscriptionPort;
        this.checkoutPort = checkoutPort;
        this.disputePort = disputePort;
        this.providerSpecificPort = providerSpecificPort;
        this.reconciliationPort = reconciliationPort;
    }
    
    @Override public PaymentPort payments() { return paymentPort; }
    @Override public RefundPort refunds() { return refundPort; }
    @Override public PayoutPort payouts() { return payoutPort; }
    @Override public CustomerPort customers() { return customerPort; }
    @Override public SubscriptionPort subscriptions() { return subscriptionPort; }
    @Override public CheckoutPort checkout() { return checkoutPort; }
    @Override public DisputePort disputes() { return disputePort; }
    @Override public ProviderSpecificPort providerSpecific() { return providerSpecificPort; }
    @Override public ReconciliationPort reconciliation() { return reconciliationPort; }
    
    @Override
    public String getProviderName() {
        return "stripe";
    }
    
    @Override
    public boolean isHealthy() {
        // Implement health check (e.g., call Stripe API to verify connectivity)
        return true;
    }
}
```

---

### Step 6: Spring Boot Configuration

```java
package com.firefly.psps.stripe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
@ComponentScan("com.firefly.psps.stripe")
public class StripeAutoConfiguration {
    // Spring Boot will auto-discover all @Component classes
}

@ConfigurationProperties(prefix = "firefly.psp.stripe")
class StripeProperties {
    private String apiKey;
    private String webhookSecret;
    private String environment = "sandbox";
    
    // Getters and setters
}
```

Create `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.firefly.psps.stripe.config.StripeAutoConfiguration
```

---

## Configuration

### application.yml

```yaml
firefly:
  psp:
    provider: stripe
    base-path: /api/psp
    stripe:
      api-key: ${STRIPE_API_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      environment: sandbox
    resilience:
      enabled: true
      circuit-breaker:
        failure-rate-threshold: 50
        minimum-number-of-calls: 10
      rate-limiter:
        limit-for-period: 50
      retry:
        max-attempts: 3
```

---

## Code Statistics

### What You Write:
- **1 Service class** (~5 lines) - Extends AbstractPspService
- **5 Controller classes** (~15 lines total) - Extend abstract controllers
- **9 Port implementations** (~500-1000 lines) - Actual PSP integration logic
- **Mapper classes** (~200-300 lines) - DTO conversions
- **Configuration** (~50 lines) - Spring Boot setup

**Total:** ~800-1,400 lines of code

### What You Get:
- **22 REST endpoints** - Fully functional API
- **20+ service methods** - With logging and error handling
- **Enterprise resilience** - Circuit breaker, retry, rate limiter, etc.
- **Observability** - Metrics, health checks, structured logging
- **Security** - PCI-DSS compliance, GDPR support
- **Type-safety** - Compile-time validation
- **Reactive** - Non-blocking, scalable

---

## Testing Your Implementation

```java
@SpringBootTest
class StripePaymentPortTest {
    
    @Autowired
    private PaymentPort paymentPort;
    
    @Test
    void shouldCreatePayment() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(Money.fromCents(10000, Currency.USD))
            .customerId("cus_test123")
            .build();
        
        PaymentResponse response = paymentPort.createPayment(request)
            .block()
            .getBody();
        
        assertNotNull(response.getPaymentId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
    }
}
```

---

## Summary

### Implementation Effort:

| Component | Complexity | Lines of Code | Effort |
|-----------|------------|---------------|--------|
| Service Layer | **EASY** | ~5 lines | 5 minutes |
| Controllers | **EASY** | ~15 lines | 10 minutes |
| Port Implementations | **MEDIUM** | ~800 lines | 2-3 days |
| Mappers | **EASY** | ~300 lines | 1 day |
| Configuration | **EASY** | ~50 lines | 30 minutes |
| **TOTAL** | | **~1,200 lines** | **3-5 days** |

### What You Get:
- ✅ Complete REST API (22 endpoints)
- ✅ Enterprise resilience patterns
- ✅ Observability (metrics, health checks)
- ✅ Security & compliance
- ✅ Type-safe, reactive, production-ready

### ROI:
**Write ~1,200 lines → Get ~8,500 lines worth of functionality**

**Productivity Multiplier: 7x**

---

## Key Principles

1. **Extend, Don't Rewrite**: Inherit from abstract classes to get functionality for free
2. **Implement Ports Only**: Focus your effort on PSP-specific integration logic
3. **Map DTOs**: Convert between lib-psps DTOs and provider DTOs
4. **Configure**: Let Spring Boot wire everything together
5. **Test**: Write integration tests for your port implementations

---

## Next Steps

1. Clone the template: `lib-psps-{provider}-impl`
2. Implement the 9 port interfaces
3. Create mappers for DTO conversions
4. Extend the abstract service and controllers
5. Configure Spring Boot
6. Run tests

**That's it!** You have a production-ready PSP integration with 22 REST endpoints and enterprise features.
