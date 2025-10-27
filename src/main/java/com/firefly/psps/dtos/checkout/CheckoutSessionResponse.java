package com.firefly.psps.dtos.checkout;
import com.firefly.psps.domain.*;
import lombok.*;
import java.time.Instant;
import java.util.Map;
@Data @Builder public class CheckoutSessionResponse {
    private String sessionId;
    private String checkoutUrl;
    private CheckoutMode mode;
    private String status;
    private Money amount;
    private String customerId;
    private String paymentId;
    private String subscriptionId;
    private Instant expiresAt;
    private Instant createdAt;
    private Map<String, String> metadata;
    private String providerSessionId;
}
