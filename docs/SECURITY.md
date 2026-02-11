# Security Best Practices

This guide provides security recommendations when using the `library-psps` library for payment processing.

---

## Table of Contents

- [PCI-DSS Compliance](#pci-dss-compliance)
- [GDPR Compliance](#gdpr-compliance)
- [API Key Management](#api-key-management)
- [Data Security](#data-security)
- [Network Security](#network-security)
- [Audit & Monitoring](#audit--monitoring)
- [Incident Response](#incident-response)

---

## PCI-DSS Compliance

### Never Store Sensitive Card Data

**Rule**: NEVER store, log, or transmit unencrypted card data.

**What NOT to do**:
```java
// ❌ NEVER store full card numbers
database.save(Payment.builder()
    .cardNumber("4242424242424242")  // VIOLATION
    .cvv("123")  // VIOLATION
    .build());

// ❌ NEVER log sensitive data
log.info("Processing card: {}", request.getCardNumber());  // VIOLATION
```

**What to do**:
```java
// ✅ Always use tokens from PSP
database.save(Payment.builder()
    .paymentMethodToken("pm_1234abcd")  // Token reference
    .last4("4242")  // Last 4 digits only
    .build());

// ✅ Log only non-sensitive data
log.info("Processing payment for customer: {}", request.getCustomerId());
```

---

### Use Data Masking

The library provides automatic sensitive data masking:

```java
@Autowired
private ComplianceService complianceService;

public void logPaymentDetails(PaymentRequest request) {
    // Mask card numbers
    String maskedCard = complianceService.maskSensitiveData(
        request.getCardNumber(), 
        SensitiveDataType.CARD_NUMBER
    );
    log.info("Payment with card: {}", maskedCard);  // Logs: ****1234
    
    // Mask emails
    String maskedEmail = complianceService.maskSensitiveData(
        request.getEmail(),
        SensitiveDataType.EMAIL
    );
    log.info("Customer email: {}", maskedEmail);  // Logs: j***@example.com
}
```

**Supported masking types**:
- `CARD_NUMBER` - Shows last 4 digits
- `CVV` - Completely masked
- `EMAIL` - Shows first letter and domain
- `PHONE` - Shows last 4 digits
- `SSN` - Shows last 4 digits

---

### Implement Audit Logging

**Requirement**: Log ALL payment-related operations for audit trail.

```java
@Service
public class SecurePaymentService {
    
    @Autowired
    private ComplianceService complianceService;
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request, String userId) {
        // Log BEFORE operation
        complianceService.logAuditEvent(AuditEvent.builder()
            .eventType("PAYMENT_INITIATED")
            .userId(userId)
            .tenantId(getTenantId())
            .resourceId(request.getCustomerId())
            .action("CREATE_PAYMENT")
            .metadata(Map.of(
                "amount", request.getAmount().toString(),
                "currency", request.getAmount().getCurrency().name()
            ))
            .ipAddress(getClientIp())
            .timestamp(Instant.now())
            .build());
        
        return pspAdapter.payments()
            .createPayment(request)
            .map(ResponseEntity::getBody)
            .doOnSuccess(payment -> {
                // Log AFTER success
                complianceService.logAuditEvent(AuditEvent.builder()
                    .eventType("PAYMENT_CREATED")
                    .userId(userId)
                    .resourceId(payment.getPaymentId())
                    .action("CREATE_PAYMENT")
                    .successful(true)
                    .timestamp(Instant.now())
                    .build());
            })
            .doOnError(error -> {
                // Log AFTER failure
                complianceService.logAuditEvent(AuditEvent.builder()
                    .eventType("PAYMENT_FAILED")
                    .userId(userId)
                    .action("CREATE_PAYMENT")
                    .successful(false)
                    .errorMessage(error.getMessage())
                    .timestamp(Instant.now())
                    .build());
            });
    }
}
```

---

### Restrict Data Access

**Principle**: Apply least-privilege access to payment data.

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        // Configure custom security expressions
        return handler;
    }
}

@Service
public class PaymentService {
    
    // Only users with PAYMENT_READ permission can view
    @PreAuthorize("hasPermission(#paymentId, 'Payment', 'READ')")
    public Mono<PaymentResponse> getPayment(String paymentId) {
        return pspAdapter.payments().getPayment(paymentId)
            .map(ResponseEntity::getBody);
    }
    
    // Only users with PAYMENT_WRITE permission can create
    @PreAuthorize("hasAuthority('PAYMENT_WRITE')")
    public Mono<PaymentResponse> createPayment(CreatePaymentRequest request) {
        return pspAdapter.payments().createPayment(request)
            .map(ResponseEntity::getBody);
    }
    
    // Only admins can issue refunds
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<RefundResponse> createRefund(CreateRefundRequest request) {
        return pspAdapter.refunds().createRefund(request)
            .map(ResponseEntity::getBody);
    }
}
```

---

## GDPR Compliance

### Right to Erasure (Right to be Forgotten)

When a customer requests data deletion:

```java
@Service
public class GdprService {
    
    @Autowired
    private ComplianceService complianceService;
    
    @Autowired
    private PspAdapter pspAdapter;
    
    public Mono<Void> eraseCustomerData(String customerId, String requestedBy) {
        // Step 1: Export data for records
        return complianceService.exportCustomerData(customerId)
            .flatMap(export -> {
                // Save export for legal compliance (30-90 days)
                return saveComplianceRecord(export);
            })
            // Step 2: Anonymize customer in PSP
            .then(complianceService.anonymizeCustomerData(customerId))
            // Step 3: Delete from local database
            .then(deleteLocalCustomerData(customerId))
            // Step 4: Log the deletion
            .doOnSuccess(v -> {
                complianceService.logAuditEvent(AuditEvent.builder()
                    .eventType("GDPR_ERASURE")
                    .userId(requestedBy)
                    .resourceId(customerId)
                    .action("DELETE_CUSTOMER_DATA")
                    .successful(true)
                    .timestamp(Instant.now())
                    .build());
            });
    }
}
```

---

### Right to Data Portability

Allow customers to export their data:

```java
@RestController
@RequestMapping("/api/gdpr")
public class GdprController {
    
    @Autowired
    private ComplianceService complianceService;
    
    @GetMapping("/export")
    @PreAuthorize("@gdprAuthorizer.canExport(#customerId, authentication)")
    public Mono<ResponseEntity<CustomerDataExport>> exportData(
            @RequestParam String customerId) {
        
        return complianceService.exportCustomerData(customerId)
            .map(export -> {
                // Set headers for download
                return ResponseEntity.ok()
                    .header("Content-Disposition", 
                        "attachment; filename=customer-data-" + customerId + ".json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(export);
            });
    }
}
```

---

## API Key Management

### Never Hardcode Keys

**Wrong**:
```java
// ❌ NEVER hardcode API keys
@Value("sk_live_abc123...")
private String stripeKey;
```

**Correct**:
```yaml
# application.yml
firefly:
  psp:
    stripe:
      api-key: ${STRIPE_API_KEY}  # From environment variable
```

```bash
# Set in environment
export STRIPE_API_KEY=sk_live_...

# Or use secrets management
export STRIPE_API_KEY=$(aws secretsmanager get-secret-value \
  --secret-id prod/stripe/api-key \
  --query SecretString \
  --output text)
```

---

### Rotate Keys Regularly

Implement key rotation:

```java
@Configuration
public class PspKeyRotation {
    
    @Scheduled(cron = "0 0 2 1 * ?")  // First day of month at 2 AM
    public void checkKeyRotation() {
        // Check key age from secrets manager
        Instant keyCreated = getKeyCreationDate();
        Duration age = Duration.between(keyCreated, Instant.now());
        
        if (age.toDays() > 90) {
            // Alert for key rotation
            alertOps("PSP API key is older than 90 days. Rotation required.");
        }
    }
}
```

---

### Use Different Keys per Environment

```yaml
# application-dev.yml
firefly:
  psp:
    stripe:
      api-key: ${STRIPE_TEST_KEY}  # sk_test_...

# application-prod.yml
firefly:
  psp:
    stripe:
      api-key: ${STRIPE_LIVE_KEY}  # sk_live_...
```

**Verification**:
```java
@PostConstruct
public void verifyEnvironment() {
    String key = pspProperties.getStripe().getApiKey();
    
    if (key.startsWith("sk_live_") && !isProdEnvironment()) {
        throw new IllegalStateException(
            "Live API key detected in non-production environment!");
    }
    
    if (key.startsWith("sk_test_") && isProdEnvironment()) {
        throw new IllegalStateException(
            "Test API key detected in production environment!");
    }
}
```

---

## Data Security

### Encrypt Data at Rest

For any payment-related data you store:

```java
@Entity
public class PaymentRecord {
    
    @Id
    private String id;
    
    // Only store token, never raw card data
    @Column(nullable = false)
    private String paymentMethodToken;
    
    // Encrypt customer PII
    @Convert(converter = EncryptedStringConverter.class)
    private String customerEmail;
    
    // Never store CVV
    // @Column private String cvv;  // ❌ NEVER
    
    // Last 4 digits OK for display
    @Column
    private String last4;
}

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

---

### Use TLS/HTTPS Only

**Enforce HTTPS**:
```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .requiresChannel()
                .anyRequest().requiresSecure()  // Force HTTPS
            .and()
            .headers()
                .httpStrictTransportSecurity()
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000);  // HSTS
    }
}
```

---

## Network Security

### IP Whitelisting

Restrict PSP webhook endpoints:

```java
@Configuration
public class WebhookSecurityConfig {
    
    // Stripe webhook IPs (example)
    private static final List<String> STRIPE_IPS = List.of(
        "3.18.12.63",
        "3.130.192.231",
        "13.235.14.237"
        // ... more IPs
    );
    
    @Bean
    public SecurityFilterChain webhookFilterChain(HttpSecurity http) throws Exception {
        http
            .requestMatchers(matchers -> matchers
                .antMatchers("/webhooks/**"))
            .authorizeRequests(authorize -> authorize
                .requestMatchers(new IpAddressWhitelistMatcher(STRIPE_IPS))
                .permitAll()
                .anyRequest().denyAll());
        
        return http.build();
    }
}
```

---

### Rate Limiting

Protect against abuse:

```yaml
firefly:
  psp:
    resilience:
      rate-limiter:
        limit-for-period: 100      # Max 100 requests
        limit-refresh-period: PT1S # Per second
        timeout-duration: PT0S     # Don't wait, fail immediately
```

---

## Audit & Monitoring

### Monitor for Suspicious Activity

```java
@Service
public class FraudDetectionService {
    
    @Autowired
    private ComplianceService complianceService;
    
    public Mono<Boolean> checkTransaction(TransactionContext context) {
        // AML/KYC checks
        if (complianceService.requiresEnhancedDueDiligence(context)) {
            // Flag for manual review
            alertCompliance("Enhanced due diligence required", context);
            return Mono.just(false);
        }
        
        // Velocity checks
        if (context.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            // Large transaction - extra verification
            return performEnhancedVerification(context);
        }
        
        return Mono.just(true);
    }
}
```

---

### Set Up Alerts

```java
@Configuration
public class SecurityAlerting {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @EventListener
    public void onPaymentFailure(PaymentFailedEvent event) {
        meterRegistry.counter("payment.failures",
            "reason", event.getReason(),
            "severity", calculateSeverity(event)
        ).increment();
        
        // Alert on suspicious patterns
        if (event.getReason().equals("STOLEN_CARD")) {
            alertSecurityTeam("Possible fraud detected", event);
        }
    }
    
    @Scheduled(fixedRate = 60000)  // Every minute
    public void checkFailureRate() {
        double failureRate = calculateFailureRate();
        
        if (failureRate > 0.10) {  // >10% failure rate
            alertOps("High payment failure rate: " + failureRate);
        }
    }
}
```

---

## Incident Response

### Have a Response Plan

**When a security incident occurs**:

**Step 1: Contain**
```java
// Immediately disable affected accounts
public void respondToIncident(String incidentId) {
    // Revoke API keys
    revokeApiKey(currentApiKey);
    
    // Disable affected customer accounts
    affectedCustomers.forEach(this::disableAccount);
    
    // Enable enhanced monitoring
    enableEnhancedLogging();
}
```

**Step 2: Investigate**
```java
// Audit log query
public List<AuditEvent> investigateIncident(String customerId, Instant from, Instant to) {
    return auditLogRepository.findByResourceIdAndTimestampBetween(
        customerId, from, to
    );
}
```

**Step 3: Notify**
```java
// Notify affected customers (GDPR requirement)
public void notifyCustomers(List<String> affectedCustomerIds) {
    affectedCustomerIds.forEach(customerId -> {
        emailService.send(Email.builder()
            .to(getCustomerEmail(customerId))
            .subject("Security Notice")
            .body(getSecurityNoticeTemplate())
            .build());
    });
}
```

**Step 4: Remediate**
```java
// Rotate all keys
public void rotateAllKeys() {
    String newKey = secretsManager.createNewKey();
    secretsManager.updateKey("psp.api-key", newKey);
    
    // Update in all environments
    deployNewKeyToAllEnvironments(newKey);
}
```

---

## Security Checklist

Before going to production:

- [ ] No sensitive data in logs
- [ ] All API keys in environment variables
- [ ] TLS/HTTPS enforced
- [ ] Audit logging enabled
- [ ] Rate limiting configured
- [ ] IP whitelisting for webhooks
- [ ] Data encryption at rest
- [ ] Secrets rotation policy defined
- [ ] Incident response plan documented
- [ ] Security monitoring/alerts configured
- [ ] PCI-DSS compliance verified
- [ ] GDPR processes implemented
- [ ] Security training completed
- [ ] Penetration testing performed
- [ ] Compliance audit passed

---

## Resources

- [PCI-DSS Requirements](https://www.pcisecuritystandards.org/)
- [GDPR Official Text](https://gdpr-info.eu/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Stripe Security](https://stripe.com/docs/security)
- [Adyen Security](https://www.adyen.com/knowledge-hub/security)

---

**Last Updated**: 2025-10-27  
**Library Version**: 1.0.0-SNAPSHOT
