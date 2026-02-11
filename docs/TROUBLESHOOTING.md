# Troubleshooting Guide

This guide helps you diagnose and fix common issues when using the `lib-psps` library.

---

## Table of Contents

- [Build & Compilation Issues](#build--compilation-issues)
- [Runtime Configuration Issues](#runtime-configuration-issues)
- [Reactive Programming Issues](#reactive-programming-issues)
- [Resilience Pattern Issues](#resilience-pattern-issues)
- [PSP Integration Issues](#psp-integration-issues)
- [Performance Issues](#performance-issues)
- [Security & Compliance Issues](#security--compliance-issues)

---

## Build & Compilation Issues

### Issue: `ClassNotFoundException` for Resilience4j Classes

**Symptom**:
```
java.lang.ClassNotFoundException: io.github.resilience4j.circuitbreaker.CircuitBreaker
```

**Cause**: Missing Resilience4j dependencies

**Solution**:
```xml
<!-- Add to your pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

**Verification**:
```bash
mvn dependency:tree | grep resilience4j
```

---

### Issue: Javadoc Warnings About Missing `@param` Tags

**Symptom**:
```
warning: no @param for field in record
```

**Cause**: Java records don't require `@param` tags (they're self-documenting)

**Solution**: This is expected and suppressed in the library configuration. If you see these warnings in your implementation:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    <configuration>
        <doclint>all,-missing</doclint>
    </configuration>
</plugin>
```

---

### Issue: Compilation Error - "Cannot Find Symbol: PspAdapter"

**Symptom**:
```
error: cannot find symbol PspAdapter
```

**Cause**: Missing library dependency

**Solution**:
```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-psps</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Verification**:
```bash
mvn clean compile
```

---

## Runtime Configuration Issues

### Issue: "No Qualifying Bean of Type 'PspAdapter'"

**Symptom**:
```
NoSuchBeanDefinitionException: No qualifying bean of type 'PspAdapter'
```

**Cause**: No PSP implementation on the classpath

**Solution**:

**Step 1**: Add a PSP implementation dependency
```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-psps-stripe-impl</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>runtime</scope>
</dependency>
```

**Step 2**: Configure the provider
```yaml
firefly:
  psp:
    provider: stripe
```

**Verification**:
```bash
# Check if beans are created
curl http://localhost:8080/actuator/beans | grep PspAdapter
```

---

### Issue: Circuit Breaker Configuration Not Applied

**Symptom**: Circuit breaker uses default values instead of your configuration

**Cause**: Property names don't match or resilience is disabled

**Solution**:

**Check** that resilience is enabled:
```yaml
firefly:
  psp:
    resilience:
      enabled: true  # Must be true
```

**Check** property names match exactly:
```yaml
firefly:
  psp:
    resilience:
      circuit-breaker:
        failure-rate-threshold: 50
        minimum-number-of-calls: 10
```

**Verification**:
```java
@Autowired
private CircuitBreakerRegistry circuitBreakerRegistry;

public void checkConfig() {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("my-psp-operation");
    System.out.println("Failure rate: " + cb.getCircuitBreakerConfig().getFailureRateThreshold());
}
```

---

## Reactive Programming Issues

### Issue: Mono/Flux Never Completes

**Symptom**: Your code hangs and never returns a response

**Cause**: Not subscribing to the Mono/Flux

**Wrong**:
```java
// ❌ This does NOTHING - Mono is lazy
Mono<PaymentResponse> payment = pspAdapter.payments().createPayment(request);
// Code continues without waiting for payment
```

**Correct** (WebFlux Controller):
```java
// ✅ Spring WebFlux subscribes automatically
@PostMapping("/payments")
public Mono<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
    return pspAdapter.payments()
        .createPayment(request)
        .map(ResponseEntity::getBody);  // WebFlux will subscribe
}
```

**Correct** (Blocking for Tests):
```java
// ✅ For tests/synchronous contexts
PaymentResponse payment = pspAdapter.payments()
    .createPayment(request)
    .block();  // Explicitly subscribe and wait
```

---

### Issue: `IllegalStateException`: "block() is blocking, which is not supported"

**Symptom**:
```
IllegalStateException: block()/blockFirst()/blockLast() are blocking, which is not supported in thread reactor-http-nio-2
```

**Cause**: Calling `.block()` inside a reactive chain

**Wrong**:
```java
return Mono.fromCallable(() -> {
    // ❌ Blocking inside reactive context
    PaymentResponse payment = pspAdapter.payments()
        .createPayment(request)
        .block();
    return payment;
});
```

**Correct**:
```java
// ✅ Chain reactively
return pspAdapter.payments()
    .createPayment(request)
    .map(ResponseEntity::getBody)
    .flatMap(payment -> {
        // Continue reactive chain
        return additionalProcessing(payment);
    });
```

---

### Issue: Reactor Context Lost

**Symptom**: Tenant context or correlation ID disappears mid-chain

**Cause**: Not propagating context properly

**Solution**:
```java
return pspAdapter.payments()
    .createPayment(request)
    .contextWrite(Context.of("tenantId", tenantId))  // Add context
    .map(response -> {
        // Context available here
        String tenant = Mono.deferContextual(ctx -> 
            Mono.just(ctx.get("tenantId"))).block();
        return response;
    });
```

---

## Resilience Pattern Issues

### Issue: Circuit Breaker Opens Immediately

**Symptom**: Circuit breaker opens after just a few failures

**Cause**: `minimum-number-of-calls` is too low

**Solution**:
```yaml
firefly:
  psp:
    resilience:
      circuit-breaker:
        minimum-number-of-calls: 20  # Need at least 20 calls before calculating failure rate
        failure-rate-threshold: 50   # Then open if >50% fail
```

**Debugging**:
```java
@Autowired
private CircuitBreakerRegistry registry;

public void debug() {
    CircuitBreaker cb = registry.circuitBreaker("stripe-payment");
    CircuitBreaker.Metrics metrics = cb.getMetrics();
    
    System.out.println("State: " + cb.getState());
    System.out.println("Failure rate: " + metrics.getFailureRate());
    System.out.println("Number of calls: " + metrics.getNumberOfBufferedCalls());
}
```

---

### Issue: Rate Limiter Blocks All Requests

**Symptom**: Getting `RequestNotPermitted` exceptions

**Cause**: Rate limit too restrictive

**Solution**:
```yaml
firefly:
  psp:
    resilience:
      rate-limiter:
        limit-for-period: 100        # Allow 100 calls
        limit-refresh-period: PT1S   # Per second
        timeout-duration: PT5S       # Wait up to 5s for permission
```

**Monitoring**:
```java
RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("stripe-payment");
RateLimiter.Metrics metrics = rateLimiter.getMetrics();

System.out.println("Available permissions: " + metrics.getAvailablePermissions());
System.out.println("Waiting threads: " + metrics.getNumberOfWaitingThreads());
```

---

### Issue: Retry Exhausted Too Quickly

**Symptom**: Operations fail after only 1 or 2 retries

**Cause**: Default retry configuration

**Solution**:
```yaml
firefly:
  psp:
    resilience:
      retry:
        max-attempts: 5                    # Try 5 times total
        wait-duration: PT1S                # Wait 1s between retries
        exponential-backoff-multiplier: 2  # Double wait time each retry
        # Retry sequence: 1s, 2s, 4s, 8s
```

**Selective Retry**:
```java
// Only retry on specific exceptions
@Retry(name = "payment", fallbackMethod = "fallback")
@Component
public class PaymentService {
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        return pspAdapter.payments().createPayment(request);
    }
    
    // Fallback when all retries exhausted
    public Mono<PaymentResponse> fallback(CreatePaymentRequest request, Exception ex) {
        log.error("Payment failed after retries", ex);
        return Mono.error(new PaymentFailedException("Service temporarily unavailable"));
    }
}
```

---

## PSP Integration Issues

### Issue: PSP Returns 401 Unauthorized

**Symptom**:
```
PspAuthenticationException: 401 Unauthorized
```

**Diagnostic Steps**:

**Step 1**: Check API key format
```yaml
# ❌ Wrong - includes quotes
firefly:
  psp:
    stripe:
      api-key: "sk_test_..."

# ✅ Correct
firefly:
  psp:
    stripe:
      api-key: ${STRIPE_API_KEY}
```

**Step 2**: Verify environment
```bash
# Check which environment key you're using
echo $STRIPE_API_KEY
# sk_test_... = Test mode
# sk_live_... = Live mode
```

**Step 3**: Test key directly
```bash
curl https://api.stripe.com/v1/charges \
  -u ${STRIPE_API_KEY}:
```

---

### Issue: Payment Succeeds But Webhook Never Arrives

**Symptom**: Payment created successfully, but webhook handler never called

**Cause**: Webhooks are NOT handled by this library

**Solution**: Use `core-common-webhooks-mgmt` for webhook handling:

```java
// This library handles API calls:
pspAdapter.payments().createPayment(request);  // ✅ Works

// Separate webhook service handles events:
// - Receives webhook from PSP
// - Verifies signature
// - Publishes event to your application
```

**Architecture**:
```
Your App ──API calls──> lib-psps ──> PSP
                                      │
PSP ──webhooks──> webhooks-mgmt ──events──> Your App
```

---

### Issue: Currency Not Supported

**Symptom**:
```
PaymentValidationException: Currency JPY not supported by this PSP
```

**Solution**:

**Step 1**: Check supported currencies
```java
@Autowired
private PaymentValidator validator;

Set<Currency> supported = validator.getSupportedCurrencies();
System.out.println("Supported: " + supported);
```

**Step 2**: Add currency support in your adapter
```java
@Override
public Set<Currency> getSupportedCurrencies() {
    return Set.of(
        Currency.USD,
        Currency.EUR,
        Currency.GBP,
        Currency.JPY  // Add JPY
    );
}
```

---

## Performance Issues

### Issue: High Latency on Payment Operations

**Diagnostic Steps**:

**Step 1**: Check metrics
```bash
curl http://localhost:8080/actuator/metrics/psp.operation | jq
```

**Step 2**: Enable detailed logging
```yaml
logging:
  level:
    com.firefly.psps: DEBUG
```

**Step 3**: Check timeout configuration
```yaml
firefly:
  psp:
    resilience:
      time-limiter:
        timeout-duration: PT30S  # 30 seconds might be too long
```

**Optimization**:
```yaml
firefly:
  psp:
    resilience:
      time-limiter:
        timeout-duration: PT10S  # Reduce to 10 seconds
      bulkhead:
        max-concurrent-calls: 50 # Increase parallelism
```

---

### Issue: Memory Leaks

**Symptom**: Memory usage grows over time

**Common Causes**:

**1. Not subscribing to Mono/Flux**:
```java
// ❌ Creates Mono but never executes - potential leak
Mono<PaymentResponse> mono = pspAdapter.payments().createPayment(request);
// Mono is cached somewhere and never cleaned up
```

**2. Blocking without timeout**:
```java
// ❌ Might block forever
payment.block();

// ✅ Always use timeout
payment.block(Duration.ofSeconds(30));
```

**3. Context not cleaned up**:
```java
// ✅ Use try-finally for context
Context ctx = Context.of("key", value);
try {
    return operation().contextWrite(ctx);
} finally {
    // Cleanup if needed
}
```

---

## Security & Compliance Issues

### Issue: Sensitive Data Appears in Logs

**Symptom**: Card numbers visible in application logs

**Solution**:

**Step 1**: Enable compliance service
```java
@Autowired
private ComplianceService complianceService;

public void logPayment(PaymentRequest request) {
    String masked = complianceService.maskSensitiveData(
        request.getCardNumber(), 
        SensitiveDataType.CARD_NUMBER
    );
    log.info("Processing payment for card: {}", masked);  // ****1234
}
```

**Step 2**: Configure logging framework
```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{ISO8601} %5p [%t] %c{1}: %m%n</pattern>
        </encoder>
    </appender>
    
    <!-- Never log at TRACE level in production -->
    <logger name="com.firefly.psps" level="INFO"/>
</configuration>
```

---

### Issue: PCI-DSS Compliance Audit Fails

**Common Issues**:

**1. Storing card data**:
```java
// ❌ NEVER store raw card data
database.save(payment.getCardNumber());

// ✅ Always use tokens
database.save(payment.getPaymentMethodToken());
```

**2. Logging CVV**:
```java
// ❌ NEVER log CVV
log.info("CVV: {}", request.getCvv());

// ✅ Don't log CVV at all
log.info("Processing payment for customer: {}", request.getCustomerId());
```

**3. Missing audit trail**:
```java
// ✅ Always log audit events
complianceService.logAuditEvent(AuditEvent.builder()
    .eventType("PAYMENT_CREATED")
    .userId(userId)
    .resourceId(paymentId)
    .build());
```

---

## Getting More Help

If your issue isn't covered here:

1. **Check the logs**: Enable DEBUG logging for `com.firefly.psps`
2. **Review metrics**: Check `/actuator/metrics` for anomalies
3. **Test in isolation**: Create a minimal reproduction
4. **Check PSP documentation**: Verify the PSP-specific behavior
5. **Review the test suite**: See how the library is meant to be used

### Useful Commands

```bash
# Check Spring Boot beans
curl localhost:8080/actuator/beans | jq '.contexts.application.beans | keys'

# Check health
curl localhost:8080/actuator/health | jq

# Check metrics
curl localhost:8080/actuator/metrics | jq

# Check circuit breaker state
curl localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state | jq
```

---

**Last Updated**: 2025-10-27  
**Library Version**: 1.0.0-SNAPSHOT
