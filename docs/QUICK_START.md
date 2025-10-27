# Quick Start Guide

Get started with the Firefly PSP library in 5 minutes.

## For Application Developers

### Step 1: Add Dependency

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-psps</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-psps-stripe-impl</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>runtime</scope>
</dependency>
```

### Step 2: Configure

```yaml
firefly:
  psp:
    provider: stripe
    base-path: /api/psp
    stripe:
      api-key: ${STRIPE_API_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
```

### Step 3: Use in Code

```java
@Service
public class OrderService {
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<PaymentResponse> processPayment(OrderRequest order) {
        return pspAdapter.payments()
            .createPayment(CreatePaymentRequest.builder()
                .amount(new Money(order.getAmount(), Currency.EUR))
                .customerId(order.getCustomerId())
                .description("Order #" + order.getId())
                .build())
            .map(ResponseEntity::getBody);
    }
}
```

## Common Use Cases

### E-Commerce Checkout

```java
@PostMapping("/checkout")
public Mono<String> checkout(@RequestBody Cart cart) {
    CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
        .mode(CheckoutMode.PAYMENT)
        .lineItems(cart.toLineItems())
        .successUrl("https://myshop.com/success")
        .cancelUrl("https://myshop.com/cart")
        .build();

    return pspAdapter.checkout()
        .createCheckoutSession(request)
        .map(resp -> "redirect:" + resp.getBody().getCheckoutUrl());
}
```

### Mobile Payment Intent

```java
@PostMapping("/payment-intent")
public Mono<PaymentIntentResponse> createIntent(@RequestBody PaymentRequest req) {
    return pspAdapter.checkout()
        .createPaymentIntent(CreatePaymentIntentRequest.builder()
            .amount(req.getAmount())
            .customerId(req.getCustomerId())
            .build())
        .map(ResponseEntity::getBody);
}
```

### Subscriptions

```java
@PostMapping("/subscribe")
public Mono<SubscriptionResponse> subscribe(@RequestBody SubscribeRequest req) {
    return pspAdapter.subscriptions()
        .createSubscription(CreateSubscriptionRequest.builder()
            .customerId(req.getCustomerId())
            .planId("plan_monthly")
            .paymentMethodId(req.getPaymentMethodId())
            .build())
        .map(ResponseEntity::getBody);
}
```

### Refunds

```java
@PostMapping("/refunds")
public Mono<RefundResponse> refund(@RequestBody RefundRequest req) {
    return pspAdapter.refunds()
        .createRefund(CreateRefundRequest.builder()
            .paymentId(req.getPaymentId())
            .amount(req.getAmount())
            .reason(req.getReason())
            .build())
        .map(ResponseEntity::getBody);
}
```

## Auto-Configured REST Endpoints

When you add a PSP implementation dependency, these endpoints are automatically available:

```
POST   /api/psp/payments              # Create payment
GET    /api/psp/payments/{id}         # Get payment
POST   /api/psp/payments/{id}/confirm # Confirm payment
POST   /api/psp/payments/{id}/capture # Capture payment
POST   /api/psp/payments/{id}/cancel  # Cancel payment

POST   /api/psp/subscriptions            # Create subscription
GET    /api/psp/subscriptions/{id}       # Get subscription
POST   /api/psp/subscriptions/{id}/cancel # Cancel subscription

POST   /api/psp/webhooks  # Webhook handler (with signature verification)
```

## Testing

```java
@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PspAdapter pspAdapter;

    @Test
    void testCreatePayment() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(new Money(BigDecimal.valueOf(100), Currency.EUR))
            .customerId("test-customer")
            .build();

        PaymentResponse response = pspAdapter.payments()
            .createPayment(request)
            .block()
            .getBody();

        assertNotNull(response.getPaymentId());
    }
}
```

## Next Steps

- Review **[Architecture](ARCHITECTURE.md)** to understand the design
- Follow **[Implementation Pattern](IMPLEMENTATION_PATTERN.md)** to create a PSP adapter (7x productivity multiplier)
- Explore **[Enterprise Architecture](ARCHITECTURE_ENTERPRISE.md)** for resilience and compliance features
