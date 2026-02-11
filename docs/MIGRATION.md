# Migration Guide

This guide helps you migrate from direct Payment Service Provider (PSP) integrations to the `library-psps` abstraction library.

---

## Table of Contents

- [Why Migrate?](#why-migrate)
- [Before You Start](#before-you-start)
- [Migration Paths](#migration-paths)
  - [From Stripe](#from-stripe)
  - [From Adyen](#from-adyen)
  - [From PayPal](#from-paypal)
  - [From Legacy Internal Code](#from-legacy-internal-code)
- [Step-by-Step Migration](#step-by-step-migration)
- [Testing Your Migration](#testing-your-migration)
- [Rollback Plan](#rollback-plan)
- [Common Issues](#common-issues)
- [FAQ](#faq)

---

## Why Migrate?

### Benefits of library-psps

- **PSP Independence**: Switch payment providers without code changes
- **Cost Optimization**: Automatic routing to lowest-cost provider
- **Resilience**: Built-in retry, circuit breaker, and fallback mechanisms
- **Compliance**: PCI-DSS and GDPR compliance out of the box
- **Observability**: Unified metrics, logging, and tracing
- **Multi-tenancy**: Support multiple customers with different PSPs
- **Simplified Code**: Less boilerplate, more business logic

---

## Before You Start

### Prerequisites

✅ Java 17 or higher  
✅ Spring Boot 3.x  
✅ Existing PSP account (Stripe, Adyen, etc.)  
✅ Database for migration tracking (optional but recommended)  
✅ Test environment for validation

### Compatibility Matrix

| Your Current Stack | library-psps Support | Notes |
|--------------------|------------------|-------|
| Stripe SDK v20+   | ✅ Full          | Direct mapping |
| Adyen SDK v16+    | ✅ Full          | Direct mapping |
| PayPal SDK v2     | ✅ Full          | Direct mapping |
| Custom PSP        | ⚠️ Adapter needed | Implement `PspAdapter` |
| Older SDKs        | ⚠️ May require updates | Check version compatibility |

---

## Migration Paths

### From Stripe

#### Before (Direct Stripe SDK)

```java
// Old code using Stripe SDK directly
@Service
public class OldPaymentService {
    
    private final StripeClient stripeClient;
    
    public OldPaymentService() {
        Stripe.apiKey = "sk_test_...";
        this.stripeClient = new StripeClient(Stripe.apiKey);
    }
    
    public PaymentIntent createPayment(String customerId, Long amount) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setCustomer(customerId)
            .setAmount(amount)
            .setCurrency("usd")
            .setPaymentMethod("pm_card_visa")
            .setConfirm(true)
            .build();
        
        return PaymentIntent.create(params);
    }
    
    public Refund refundPayment(String paymentIntentId, Long amount) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)
            .setAmount(amount)
            .build();
        
        return Refund.create(params);
    }
}
```

#### After (library-psps)

```java
// New code using library-psps
@Service
public class PaymentService {
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<PaymentResponse> createPayment(String customerId, BigDecimal amount) {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .customerId(customerId)
            .amount(Money.of(amount, Currency.USD))
            .paymentMethodId("pm_card_visa")
            .captureMethod(CaptureMethod.AUTOMATIC)
            .build();
        
        return pspAdapter.payments()
            .createPayment(request)
            .map(ResponseEntity::getBody);
    }
    
    public Mono<RefundResponse> refundPayment(String paymentId, BigDecimal amount) {
        CreateRefundRequest request = CreateRefundRequest.builder()
            .paymentId(paymentId)
            .amount(Money.of(amount, Currency.USD))
            .reason(RefundReason.REQUESTED_BY_CUSTOMER)
            .build();
        
        return pspAdapter.refunds()
            .createRefund(request)
            .map(ResponseEntity::getBody);
    }
}
```

#### Configuration Migration

**Before (hardcoded)**:
```java
Stripe.apiKey = "sk_test_...";
```

**After (application.yml)**:
```yaml
firefly:
  psp:
    provider: stripe  # or multi-psp with routing
    stripe:
      api-key: ${STRIPE_API_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
    resilience:
      retry:
        max-attempts: 3
        wait-duration: PT2S
```

---

### From Adyen

#### Before (Direct Adyen SDK)

```java
@Service
public class OldAdyenService {
    
    private final Client client;
    private final Checkout checkout;
    
    public OldAdyenService() {
        Config config = new Config();
        config.setApiKey("AQE...");
        config.setEnvironment(Environment.TEST);
        this.client = new Client(config);
        this.checkout = new Checkout(client);
    }
    
    public PaymentsResponse createPayment(String merchantAccount, Long amount) 
            throws ApiException, IOException {
        
        PaymentMethodsRequest paymentMethodRequest = new PaymentMethodsRequest();
        paymentMethodRequest.setMerchantAccount(merchantAccount);
        
        PaymentsRequest request = new PaymentsRequest();
        request.setMerchantAccount(merchantAccount);
        request.setAmount(new Amount().currency("EUR").value(amount));
        request.setReference("ORDER-" + UUID.randomUUID());
        
        return checkout.payments(request);
    }
}
```

#### After (library-psps)

```java
@Service
public class PaymentService {
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<PaymentResponse> createPayment(String merchantAccount, BigDecimal amount) {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .customerId(merchantAccount)
            .amount(Money.of(amount, Currency.EUR))
            .reference("ORDER-" + UUID.randomUUID())
            .build();
        
        return pspAdapter.payments()
            .createPayment(request)
            .map(ResponseEntity::getBody);
    }
}
```

#### Configuration Migration

**Before**:
```java
Config config = new Config();
config.setApiKey("AQE...");
config.setEnvironment(Environment.TEST);
```

**After**:
```yaml
firefly:
  psp:
    provider: adyen
    adyen:
      api-key: ${ADYEN_API_KEY}
      merchant-account: ${ADYEN_MERCHANT_ACCOUNT}
      environment: TEST
```

---

### From PayPal

#### Before (Direct PayPal SDK)

```java
@Service
public class OldPayPalService {
    
    private final PayPalHttpClient client;
    
    public OldPayPalService() {
        PayPalEnvironment environment = new SandboxEnvironment(
            "CLIENT_ID",
            "CLIENT_SECRET"
        );
        this.client = new PayPalHttpClient(environment);
    }
    
    public Order createOrder(String currency, String value) throws IOException {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");
        
        AmountWithBreakdown amountBreakdown = new AmountWithBreakdown()
            .currencyCode(currency)
            .value(value);
        
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
            .amountWithBreakdown(amountBreakdown);
        
        orderRequest.purchaseUnits(Arrays.asList(purchaseUnit));
        
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.requestBody(orderRequest);
        
        HttpResponse<Order> response = client.execute(request);
        return response.result();
    }
}
```

#### After (library-psps)

```java
@Service
public class PaymentService {
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<PaymentResponse> createOrder(Currency currency, BigDecimal value) {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(Money.of(value, currency))
            .captureMethod(CaptureMethod.AUTOMATIC)
            .build();
        
        return pspAdapter.payments()
            .createPayment(request)
            .map(ResponseEntity::getBody);
    }
}
```

---

### From Legacy Internal Code

If you have custom payment code without a standard SDK:

#### Step 1: Assess Current Implementation

Document your current payment flow:
- API endpoints used
- Authentication method
- Request/response formats
- Error handling
- Webhook processing

#### Step 2: Create Custom Adapter

Implement the `PspAdapter` interface:

```java
@Component
@ConditionalOnProperty(prefix = "firefly.psp", name = "provider", havingValue = "custom")
public class CustomPspAdapter implements PspAdapter {
    
    private final WebClient webClient;
    private final CustomPspProperties properties;
    
    @Override
    public String getProviderName() {
        return "custom-psp";
    }
    
    @Override
    public PaymentOperations payments() {
        return new CustomPaymentOperations(webClient, properties);
    }
    
    @Override
    public CustomerOperations customers() {
        return new CustomCustomerOperations(webClient, properties);
    }
    
    // Implement other operations...
}
```

#### Step 3: Implement Operation Interfaces

```java
public class CustomPaymentOperations implements PaymentOperations {
    
    private final WebClient webClient;
    private final CustomPspProperties properties;
    
    @Override
    public Mono<ResponseEntity<PaymentResponse>> createPayment(CreatePaymentRequest request) {
        // Map to your PSP's API format
        CustomPaymentRequest customRequest = mapToCustomFormat(request);
        
        return webClient.post()
            .uri(properties.getBaseUrl() + "/payments")
            .header("Authorization", "Bearer " + properties.getApiKey())
            .bodyValue(customRequest)
            .retrieve()
            .toEntity(CustomPaymentResponse.class)
            .map(response -> ResponseEntity.ok(mapToStandardFormat(response.getBody())));
    }
    
    private PaymentResponse mapToStandardFormat(CustomPaymentResponse customResponse) {
        return PaymentResponse.builder()
            .paymentId(customResponse.getId())
            .status(mapStatus(customResponse.getState()))
            .amount(Money.of(
                customResponse.getAmount(),
                Currency.valueOf(customResponse.getCurrency())
            ))
            .createdAt(customResponse.getCreatedDate())
            .build();
    }
}
```

---

## Step-by-Step Migration

### Phase 1: Preparation (Week 1)

**1. Add Dependency**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>library-psps</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**2. Configure PSP**

```yaml
# application.yml
firefly:
  psp:
    provider: stripe  # Your current PSP
    stripe:
      api-key: ${STRIPE_API_KEY}
```

**3. Create Feature Flag**

```yaml
features:
  use-library-psps: false  # Start with false
```

```java
@Service
public class PaymentService {
    
    @Value("${features.use-library-psps}")
    private boolean useLibPsps;
    
    @Autowired(required = false)
    private PspAdapter pspAdapter;
    
    @Autowired
    private OldPaymentService oldService;
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        if (useLibPsps) {
            return pspAdapter.payments().createPayment(request)
                .map(ResponseEntity::getBody);
        } else {
            return Mono.fromCallable(() -> oldService.createPayment(request));
        }
    }
}
```

---

### Phase 2: Shadow Mode (Week 2-3)

Run both implementations in parallel to validate:

```java
@Service
public class ShadowPaymentService {
    
    @Autowired
    private PspAdapter newAdapter;
    
    @Autowired
    private OldPaymentService oldService;
    
    @Autowired
    private MetricsService metricsService;
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        // Primary: Old implementation
        Mono<PaymentResponse> oldResult = Mono.fromCallable(() -> 
            oldService.createPayment(request)
        );
        
        // Shadow: New implementation (don't fail main flow)
        Mono<PaymentResponse> newResult = newAdapter.payments()
            .createPayment(request)
            .map(ResponseEntity::getBody)
            .doOnError(error -> {
                metricsService.recordShadowFailure("create_payment", error);
            })
            .onErrorResume(error -> Mono.empty());  // Don't fail
        
        // Execute both and compare
        return Mono.zip(oldResult, newResult)
            .map(tuple -> {
                PaymentResponse old = tuple.getT1();
                PaymentResponse newResp = tuple.getT2();
                
                // Compare results
                if (!resultsMatch(old, newResp)) {
                    metricsService.recordResultMismatch("create_payment", old, newResp);
                }
                
                return old;  // Return old result
            })
            .onErrorResume(error -> oldResult);  // Fallback to old only
    }
    
    private boolean resultsMatch(PaymentResponse old, PaymentResponse newResp) {
        return Objects.equals(old.getStatus(), newResp.getStatus()) &&
               Objects.equals(old.getAmount(), newResp.getAmount());
    }
}
```

**Monitor for**:
- Response time differences
- Result discrepancies
- Error rate changes

---

### Phase 3: Gradual Rollout (Week 4-5)

Use percentage-based rollout:

```java
@Service
public class CanaryPaymentService {
    
    @Value("${features.library-psps-percentage}")
    private int libPspsPercentage;  // Start with 5%, gradually increase
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        boolean useNew = ThreadLocalRandom.current().nextInt(100) < libPspsPercentage;
        
        if (useNew) {
            metricsService.increment("payment.lib_psps.used");
            return newAdapter.payments().createPayment(request)
                .map(ResponseEntity::getBody)
                .doOnError(error -> {
                    // Alert on errors
                    alerting.sendAlert("library-psps migration error", error);
                });
        } else {
            metricsService.increment("payment.old_service.used");
            return Mono.fromCallable(() -> oldService.createPayment(request));
        }
    }
}
```

**Rollout Schedule**:
- Day 1-3: 5% traffic
- Day 4-7: 25% traffic
- Day 8-10: 50% traffic
- Day 11-14: 100% traffic

---

### Phase 4: Full Migration (Week 6)

Remove old code:

```java
@Service
public class PaymentService {
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        return pspAdapter.payments()
            .createPayment(request)
            .map(ResponseEntity::getBody);
    }
}
```

Clean up configuration:

```yaml
# Remove old PSP SDK configuration
# stripe:
#   api-key: ...

# Keep only library-psps config
firefly:
  psp:
    provider: stripe
    stripe:
      api-key: ${STRIPE_API_KEY}
```

---

### Phase 5: Optimization (Week 7+)

Enable advanced features:

```yaml
firefly:
  psp:
    # Enable cost optimization
    routing:
      strategy: lowest-cost
      cost-tracking:
        enabled: true
    
    # Add fallback PSP
    fallback:
      enabled: true
      provider: adyen
      trigger-on:
        - PROVIDER_ERROR
        - RATE_LIMIT_EXCEEDED
    
    # Enhance resilience
    resilience:
      circuit-breaker:
        enabled: true
        failure-rate-threshold: 50
        slow-call-duration-threshold: PT5S
```

---

## Testing Your Migration

### Unit Tests

```java
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PspAdapter pspAdapter;
    
    @Mock
    private PaymentOperations paymentOps;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @Test
    void shouldCreatePayment() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(Money.of(new BigDecimal("100.00"), Currency.USD))
            .build();
        
        PaymentResponse expectedResponse = PaymentResponse.builder()
            .paymentId("pay_123")
            .status(PaymentStatus.SUCCEEDED)
            .build();
        
        when(pspAdapter.payments()).thenReturn(paymentOps);
        when(paymentOps.createPayment(request))
            .thenReturn(Mono.just(ResponseEntity.ok(expectedResponse)));
        
        // When
        Mono<PaymentResponse> result = paymentService.createPayment(request);
        
        // Then
        StepVerifier.create(result)
            .expectNext(expectedResponse)
            .verifyComplete();
    }
}
```

---

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "firefly.psp.provider=stripe",
    "firefly.psp.stripe.api-key=sk_test_..."
})
class PaymentIntegrationTest {
    
    @Autowired
    private PaymentService paymentService;
    
    @Test
    void shouldProcessRealPayment() {
        // Use test card
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(Money.of(new BigDecimal("10.00"), Currency.USD))
            .paymentMethodId("pm_card_visa")
            .build();
        
        Mono<PaymentResponse> result = paymentService.createPayment(request);
        
        StepVerifier.create(result)
            .assertNext(response -> {
                assertNotNull(response.getPaymentId());
                assertEquals(PaymentStatus.SUCCEEDED, response.getStatus());
            })
            .verifyComplete();
    }
}
```

---

### Load Tests

```java
@Test
void loadTest() throws InterruptedException {
    int concurrentUsers = 100;
    int requestsPerUser = 10;
    
    CountDownLatch latch = new CountDownLatch(concurrentUsers * requestsPerUser);
    
    ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
    
    for (int i = 0; i < concurrentUsers; i++) {
        executor.submit(() -> {
            for (int j = 0; j < requestsPerUser; j++) {
                paymentService.createPayment(createTestRequest())
                    .doFinally(signal -> latch.countDown())
                    .subscribe();
            }
        });
    }
    
    latch.await(60, TimeUnit.SECONDS);
    executor.shutdown();
    
    // Verify metrics
    assertThat(metricsService.getCounter("payment.success").count())
        .isGreaterThan(concurrentUsers * requestsPerUser * 0.95);  // 95% success rate
}
```

---

## Rollback Plan

### Quick Rollback

If issues arise, quickly revert:

**1. Feature Flag Rollback**
```yaml
features:
  use-library-psps: false  # Disable immediately
```

**2. Configuration Rollback**
```yaml
# Restore old PSP SDK config
stripe:
  api-key: ${STRIPE_API_KEY}

# Disable library-psps
# firefly:
#   psp:
#     provider: stripe
```

**3. Code Rollback**

Keep old service available:
```java
@Service
public class PaymentService {
    
    @Autowired(required = false)
    private PspAdapter newAdapter;
    
    @Autowired
    private LegacyPaymentService legacyService;
    
    @Value("${features.use-library-psps:false}")
    private boolean useLibPsps;
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        if (useLibPsps && newAdapter != null) {
            return newAdapter.payments().createPayment(request)
                .map(ResponseEntity::getBody)
                .onErrorResume(error -> {
                    // Automatic fallback on error
                    log.error("library-psps failed, falling back to legacy", error);
                    return Mono.fromCallable(() -> legacyService.createPayment(request));
                });
        }
        
        return Mono.fromCallable(() -> legacyService.createPayment(request));
    }
}
```

---

## Common Issues

### Issue 1: Different Response Formats

**Problem**: PSP-specific fields missing in abstraction.

**Solution**: Use metadata field for PSP-specific data:

```java
PaymentResponse response = payment.getPaymentResponse();

// Access PSP-specific data
Map<String, Object> metadata = response.getMetadata();
String stripeChargeId = (String) metadata.get("stripe.charge_id");
```

---

### Issue 2: Webhook Signature Verification

**Problem**: Old webhook verification code doesn't work.

**Solution**: Use built-in verification:

```java
@RestController
@RequestMapping("/webhooks")
public class WebhookController {
    
    @Autowired
    private WebhookProcessor webhookProcessor;
    
    @PostMapping("/stripe")
    public Mono<ResponseEntity<Void>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        // Automatic verification and processing
        return webhookProcessor.processWebhook(payload, signature)
            .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}
```

---

### Issue 3: Performance Degradation

**Problem**: Slower response times after migration.

**Solution**: Tune resilience settings:

```yaml
firefly:
  psp:
    resilience:
      # Reduce timeout
      timeout:
        timeout-duration: PT10S  # Was PT30S
      
      # Disable unnecessary retries for fast operations
      retry:
        max-attempts: 1  # Was 3
```

---

## FAQ

**Q: Can I use multiple PSPs simultaneously?**  
A: Yes! Configure routing:
```yaml
firefly:
  psp:
    routing:
      strategy: lowest-cost
      providers:
        - stripe
        - adyen
```

**Q: How do I handle PSP-specific features?**  
A: Use metadata and conditional logic:
```java
if (pspAdapter.getProviderName().equals("stripe")) {
    // Stripe-specific logic
}
```

**Q: What about existing payment data?**  
A: No migration needed! Only new payments use library-psps.

**Q: Can I test without a real PSP?**  
A: Yes! Use the mock adapter:
```yaml
firefly:
  psp:
    provider: mock
```

**Q: How long does migration take?**  
A: Typically 6-8 weeks for full rollout, 2-3 weeks for simple cases.

---

## Support

- **Documentation**: [/docs/README.md](README.md)
- **Examples**: [/examples](../examples)
- **Issues**: [GitHub Issues](https://github.com/firefly/library-psps/issues)
- **Slack**: #library-psps-support

---

**Last Updated**: 2025-10-27  
**Library Version**: 1.0.0-SNAPSHOT
