# Enterprise PSP Architecture - Firefly Platform

## Overview

`lib-psps` is an **enterprise-grade Payment Service Provider abstraction library** built on hexagonal architecture principles. It provides a unified, resilient, and compliant interface for integrating with multiple PSPs (Stripe, Adyen, PayPal, etc.) while maintaining production-readiness for financial services.

---

## Architecture Layers

### 1. **Core Domain Layer** (lib-psps)
The heart of the library - PSP-agnostic business logic and contracts.

```
┌─────────────────────────────────────────────────────────────┐
│                    CORE DOMAIN LAYER                        │
├─────────────────────────────────────────────────────────────┤
│  9 PORT INTERFACES                                          │
│  ├── PaymentPort          - Direct payments (7 methods)     │
│  ├── RefundPort           - Refund management (5 methods)   │
│  ├── PayoutPort           - Fund transfers (5 methods)      │
│  ├── CustomerPort         - Customer/PM mgmt (9 methods)    │
│  ├── SubscriptionPort     - Recurring billing (16 methods)  │
│  ├── CheckoutPort         - Hosted checkout (7 methods)     │
│  ├── DisputePort          - Chargeback handling (5 methods) │
│  ├── ProviderSpecificPort - Custom PSP ops (4 methods)      │
│  └── ReconciliationPort   - Settlement reconciliation       │
│                                                              │
│  Note: Webhook handling managed by                          │
│        core-common-webhooks-mgmt                        │
│                                                              │
│  10 DOMAIN MODELS                                           │
│  ├── Money, Currency, PaymentStatus, PaymentMethodType      │
│  ├── SubscriptionStatus, CheckoutMode, BillingInterval      │
│  ├── CustomerInfo, Address                                  │
│                                                              │
│  44+ DTOs (Request/Response objects)                        │
│  11 Exception types                                         │
└─────────────────────────────────────────────────────────────┘
```

---

### 2. **Infrastructure & Resilience Layer** ✨ NEW

Production-ready patterns for fault tolerance and observability.

#### **Resilience Patterns (Resilience4j)**
```java
@Component
public class ResilientPspService {
    // Automatic application of:
    // 1. Circuit Breaker - Prevents cascading failures
    // 2. Rate Limiter    - Controls API call rate per PSP
    // 3. Retry           - Exponential backoff retries
    // 4. Bulkhead        - Limits concurrent calls
    // 5. Time Limiter    - Timeout protection
    
    public <T> Mono<T> execute(String provider, String op, Supplier<Mono<T>> operation)
}
```

**Configuration per PSP:**
- `failureRateThreshold: 50%` - Opens circuit at 50% error rate
- `limitForPeriod: 50` - Max 50 calls/second
- `maxAttempts: 3` - Retry up to 3 times with backoff
- `maxConcurrentCalls: 25` - Limit parallel calls
- `timeoutDuration: 30s` - 30 second timeout

#### **Observability (Micrometer)**
```java
// Automatic metrics collection:
- psp.operation (Timer)         - Latency per operation
- psp.operation.count (Counter) - Success/failure counts
- Tagged by: provider, operation, status, error type
```

**Health Checks:**
```json
{
  "status": "UP",
  "details": {
    "provider": "stripe",
    "available": true,
    "circuitBreakers": {
      "stripe-payment": {
        "state": "CLOSED",
        "failureRate": "2.34%",
        "numberOfCalls": 1523
      }
    }
  }
}
```

---

### 3. **Security & Compliance Layer** ✨ NEW

#### **PCI-DSS Compliance**
```java
public interface ComplianceService {
    // Mask sensitive data for logs/display
    String maskSensitiveData(String data, SensitiveDataType type);
    // ****1234, j***@example.com
    
    // Immutable audit logs
    Mono<Void> logAuditEvent(AuditEvent event);
    
    // GDPR: Right to erasure
    Mono<Void> anonymizeCustomerData(String customerId);
    
    // GDPR: Data portability
    Mono<CustomerDataExport> exportCustomerData(String customerId);
}
```

**Never logs/stores:**
- ❌ Full credit card numbers (use tokens)
- ❌ CVV/CVC codes
- ❌ Unencrypted PII

---

### 4. **Validation & Cost Optimization Layer** ✨ NEW

#### **Pre-PSP Validation**
Validates requests **before** sending to PSP (saves API calls & money):

```java
public interface PaymentValidator {
    // Validates: currency support, amount limits, payment method
    Mono<ValidationResult> validateCreatePayment(CreatePaymentRequest);
    
    boolean isCurrencySupported(Currency currency);
    boolean isAmountWithinLimits(Money amount, Currency currency);
    Set<Currency> getSupportedCurrencies();
}
```

**Catches before PSP:**
- ❌ Unsupported currencies (EUR on USD-only PSP)
- ❌ Amount below/above limits ($0.50 minimum, $999,999 max)
- ❌ Invalid payment methods

#### **Fee Calculator**
Compare costs across PSPs for routing decisions:

```java
public interface PspFeeCalculator {
    Money calculatePaymentFee(String provider, Money amount, PaymentMethodType);
    String getCheapestPsp(Money amount, PaymentMethodType, String... providers);
    Map<String, Money> compareFees(...);
}
```

**Example:**
```
Transaction: €100 via credit card
- Stripe:  €2.90 (2.9% + €0.00)
- Adyen:   €1.10 (1.1% + €0.00)
- PayPal:  €3.40 (3.4% + €0.00)
→ Route to Adyen (saves €1.80)
```

---

### 5. **Multi-Tenancy & Routing Layer** ✨ NEW

#### **Tenant Context**
Isolate PSP configurations per tenant:

```java
PspTenantContext context = PspTenantContext.builder("tenant-123")
    .withConfig("stripe.apiKey", "sk_test_abc")
    .withConfig("adyen.merchantAccount", "MerchantABC")
    .build();

return pspAdapter.payments().createPayment(request)
    .contextWrite(Context.of(TENANT_ID_KEY, context.getTenantId()));
```

#### **Intelligent PSP Router**
```java
public interface PspRouter {
    // Strategy-based PSP selection
    Mono<PspAdapter> selectPsp(RoutingContext context);
    Mono<PspAdapter> selectPspWithFailover(RoutingContext context);
}

// Routing strategies:
enum RoutingStrategy {
    FIRST_AVAILABLE,      // First healthy PSP
    CURRENCY_OPTIMIZED,   // Best exchange rates
    COST_OPTIMIZED,       // Lowest fees
    ROUND_ROBIN,          // Load balancing
    REGION_BASED,         // Geographic routing
    TENANT_SPECIFIC,      // Per-tenant override
    CUSTOM                // Your logic
}
```

**Use cases:**
1. **Failover:** Stripe down → auto-switch to Adyen
2. **Cost optimization:** Route EUR to Adyen, USD to Stripe
3. **Regional compliance:** EU customers → European PSP
4. **A/B testing:** 10% traffic to new PSP

---

### 6. **Reconciliation & Auditing Layer** ✨ NEW

#### **Payment Reconciliation**
Detect discrepancies between internal DB and PSP:

```java
public interface ReconciliationPort {
    Mono<List<PaymentDiscrepancy>> reconcilePayments(LocalDate date);
    Mono<SettlementReport> getSettlementReport(LocalDate date);
    
    enum DiscrepancyType {
        MISSING_IN_PSP,       // In DB, not in PSP
        MISSING_IN_INTERNAL,  // In PSP, not in DB
        AMOUNT_MISMATCH,      // Different amounts
        STATUS_MISMATCH,      // State inconsistency
        REFUND_MISMATCH       // Refund amounts off
    }
}
```

**Critical for:**
- ✅ Financial auditing
- ✅ Fraud detection
- ✅ Dispute resolution
- ✅ Regulatory compliance

---

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                          │
│                  (Your Microservice)                            │
└────────────────┬────────────────────────────────────────────────┘
                 │ Uses
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    LIB-PSPS (Domain)                            │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ PspAdapter (Main Entry Point)                           │  │
│  │  ├── payments()       → PaymentPort                     │
│  │  ├── refunds()        → RefundPort                      │
│  │  ├── customers()      → CustomerPort                    │
│  │  ├── subscriptions()  → SubscriptionPort                │
│  │  ├── checkout()       → CheckoutPort                    │
│  │  ├── disputes()       → DisputePort                     │
│  │  ├── reconciliation() → ReconciliationPort              │
│  │  └── providerSpecific() → ProviderSpecificPort          │
│  │                                                          │
│  │  Note: Webhooks managed by core-common-webhooks-mgmt│
│  └─────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │ Cross-Cutting Concerns                                  │  │
│  │  ├── ResilientPspService (Resilience4j wrapper)         │  │
│  │  ├── PspHealthIndicator (Spring Actuator)              │  │
│  │  ├── PaymentValidator (Pre-PSP validation)             │  │
│  │  ├── PspFeeCalculator (Cost optimization)              │  │
│  │  ├── PspRouter (Multi-PSP routing)                     │  │
│  │  ├── ComplianceService (PCI-DSS/GDPR)                  │  │
│  │  └── PspTenantContext (Multi-tenancy)                  │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │ Implemented by
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│            INFRASTRUCTURE LAYER (Adapters)                      │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Stripe       │  │ Adyen        │  │ PayPal       │  ...    │
│  │ Adapter      │  │ Adapter      │  │ Adapter      │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│         │                  │                  │                 │
│         └──────────────────┴──────────────────┘                │
│                            │                                     │
│                            ▼                                     │
│                  ┌──────────────────┐                          │
│                  │ External PSP API │                          │
│                  └──────────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Example: Resilient Payment

```
1. Client Request
   │
   ▼
2. Application → pspAdapter.payments().createPayment(request)
   │
   ▼
3. ResilientPspService.execute("stripe", "payment", () -> ...)
   │
   ├─ Circuit Breaker: Check if circuit is OPEN
   │  └─ If OPEN: Fail fast (don't call PSP)
   │
   ├─ Rate Limiter: Acquire permission
   │  └─ Wait if limit reached
   │
   ├─ Bulkhead: Acquire semaphore
   │  └─ Block if max concurrent calls reached
   │
   ├─ Time Limiter: Set timeout
   │
   └─ Retry: Wrap with retry logic
      │
      ▼
4. StripePaymentPort.createPayment(request)
   │
   ├─ Map request to Stripe format
   │
   ├─ Call Stripe API (HTTP)
   │  └─ If 429 (rate limit): Retry after backoff
   │  └─ If 5xx (server error): Retry after backoff
   │  └─ If network error: Retry after backoff
   │
   ├─ Map Stripe response to PaymentResponse
   │
   └─ Record metrics:
      - psp.operation timer (latency)
      - psp.operation.count (success/failure)
      - Circuit breaker metrics
   │
   ▼
5. Return PaymentResponse to client
```

---

## Key Design Principles

### 1. **Hexagonal Architecture (Ports & Adapters)**
- **Domain never depends on infrastructure**
- **Ports** = Interfaces defining capabilities
- **Adapters** = PSP-specific implementations
- **Testability**: Mock entire PSP in tests

### 2. **Reactive Streams (Project Reactor)**
- Non-blocking I/O with `Mono<T>` and `Flux<T>`
- Backpressure support
- Composable async operations

### 3. **Type Safety**
- Immutable value objects (`Money`, `Currency`)
- Records for DTOs
- No primitive obsession

### 4. **Fail-Fast Validation**
- Validate before PSP call (save $$)
- Clear error messages
- Business rules centralized

### 5. **Defense in Depth**
- Circuit breakers prevent cascading failures
- Rate limiters protect PSP quotas
- Retries handle transient errors
- Bulkheads isolate failures
- Timeouts prevent hanging requests

### 6. **Compliance by Design**
- PCI-DSS: Never log card data
- GDPR: Anonymization & export
- AML: Enhanced due diligence checks
- Audit: Immutable event logs

---

## Configuration Best Practices

### Development Environment
```yaml
firefly.psp:
  resilience:
    circuit-breaker:
      failure-rate-threshold: 75    # More tolerant
      minimum-number-of-calls: 5    # Trigger faster
    rate-limiter:
      limit-for-period: 10          # Low traffic
    retry:
      max-attempts: 1               # Fail fast for debugging
```

### Production Environment
```yaml
firefly.psp:
  resilience:
    circuit-breaker:
      failure-rate-threshold: 50    # Open at 50% error rate
      minimum-number-of-calls: 20   # Need more samples
      wait-duration-in-open-state: 120s  # Longer recovery time
    rate-limiter:
      limit-for-period: 100         # High throughput
    retry:
      max-attempts: 3               # More resilient
      exponential-backoff-enabled: true
```

---

## Metrics & Monitoring

### Prometheus Metrics
```
# Latency
psp_operation_seconds{provider="stripe",operation="payment",status="success"} 0.234

# Success/Failure counts
psp_operation_count{provider="stripe",operation="payment",status="success"} 1543
psp_operation_count{provider="stripe",operation="payment",status="failure"} 12

# Circuit breaker state
resilience4j_circuitbreaker_state{name="stripe-payment"} 0  # 0=CLOSED, 1=OPEN

# Fee tracking
psp_fee_total{provider="stripe",currency="EUR"} 125.50
```

### Alerting Rules (Prometheus)
```yaml
- alert: PspCircuitBreakerOpen
  expr: resilience4j_circuitbreaker_state{name=~".*-payment"} == 1
  annotations:
    summary: "PSP {{$labels.name}} circuit breaker is OPEN"

- alert: PspHighErrorRate
  expr: rate(psp_operation_count{status="failure"}[5m]) > 0.1
  annotations:
    summary: "PSP error rate >10% for {{$labels.provider}}"
```

---

## Security Considerations

### 1. **Never Log Sensitive Data**
```java
// ❌ BAD
logger.info("Payment created: cardNumber={}", cardNumber);

// ✅ GOOD
logger.info("Payment created: cardLast4={}", complianceService.maskSensitiveData(cardNumber, CREDIT_CARD));
// Logs: "cardLast4=****1234"
```

### 2. **API Keys in Environment Variables**
```yaml
# ❌ Never hardcode
firefly.psp.stripe.api-key: sk_live_abc123

# ✅ Use env vars
firefly.psp.stripe.api-key: ${STRIPE_API_KEY}
```

---

## Performance Tuning

### Concurrency Limits
```
bulkhead.max-concurrent-calls = min(
    PSP_API_LIMIT,
    YOUR_INFRASTRUCTURE_CAPACITY,
    DB_CONNECTION_POOL_SIZE
)
```

**Example:** Stripe allows 100 req/s
- Set `limit-for-period: 90` (leave 10% margin)
- Set `max-concurrent-calls: 25` (avoid overload)

### Retry Strategy
```
retry.max-attempts = 3                      # Standard for transient failures
retry.exponential-backoff-multiplier = 2.0  # Aggressive: 1s → 2s → 4s
                                             # Conservative: 1s → 1.5s → 2.25s
```

---

## Testing Strategy

### 1. **Unit Tests** (Mock Ports)
```java
@Test
void shouldCreatePayment() {
    PaymentPort mockPort = mock(PaymentPort.class);
    when(mockPort.createPayment(any())).thenReturn(Mono.just(response));
    
    // Test business logic without PSP
}
```

### 2. **Integration Tests** (Test Containers)
```java
@SpringBootTest
@Testcontainers
class PspIntegrationTest {
    @Container
    static MockServerContainer mockServer = ...;
    
    // Test against mock PSP server
}
```

### 3. **Contract Tests** (Wiremock)
```java
@Test
void shouldMatchStripeContract() {
    stubFor(post("/v1/payment_intents")
        .willReturn(okJson(stripeResponse)));
    
    // Verify adapter matches PSP contract
}
```

---

## Migration Path

### Phase 1: Add Library (Week 1)
1. Add `lib-psps` dependency
2. Configure resilience properties
3. Enable metrics endpoint

### Phase 2: Implement Adapter (Week 2-3)
1. Create `StripeAdapter implements PspAdapter`
2. Implement 11 ports
3. Add unit tests

### Phase 3: Integrate (Week 4)
1. Replace direct PSP calls with adapter
2. Add validation layer
3. Configure routing (if multi-PSP)

### Phase 4: Production Hardening (Week 5-6)
1. Load testing
2. Circuit breaker tuning
3. Alerting setup
4. Runbook documentation

---

## Summary Statistics

- **9 Port Interfaces** - Complete PSP operation coverage (Webhooks in core-common-webhooks-mgmt)
- **9 Domain Models** - Type-safe value objects (Money, Currency, Address, CustomerInfo, PaymentStatus, PaymentMethodType, SubscriptionStatus, CheckoutMode, BillingInterval)
- **41 DTOs** - Request/Response objects
- **11 Exception Types** - Fine-grained error handling
- **5 Resilience Patterns** - Production-ready fault tolerance (Circuit Breaker, Retry, Rate Limiter, Bulkhead, Time Limiter)
- **2 Security Layers** - PCI-DSS, GDPR compliance
- **7 Cross-Cutting Concerns** - Validation, routing, fees, compliance, resilience, health, multi-tenancy

**Zero boilerplate for implementations** - Inherit abstract services and controllers!

---

## Next Steps

1. ✅ Read [QUICK_START.md](QUICK_START.md) for basic usage
2. ✅ Read [IMPLEMENTATION_PATTERN.md](IMPLEMENTATION_PATTERN.md) for implementation (complete guide)
3. ✅ Review example configuration (`application-psp-example.yml`)
4. ✅ Set up metrics and alerting
5. ✅ Implement your first PSP adapter

---

**Questions?** Review the inline Javadocs or consult the implementation guide.
