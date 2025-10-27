package com.firefly.psps.dtos.subscriptions;
import lombok.*;
import java.time.Instant;
import java.util.Map;
@Data @Builder public class CreateSubscriptionRequest {
    private String customerId;
    private String planId;
    private String paymentMethodId;
    private Integer quantity;
    private Instant trialEnd;
    private Boolean cancelAtPeriodEnd;
    private Map<String, String> metadata;
}
