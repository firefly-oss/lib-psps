package com.firefly.psps.dtos.subscriptions;
import com.firefly.psps.domain.*;
import lombok.*;
import java.time.Instant;
import java.util.Map;
@Data @Builder public class SubscriptionResponse {
    private String subscriptionId;
    private String customerId;
    private String planId;
    private SubscriptionStatus status;
    private Integer quantity;
    private Money amount;
    private BillingInterval interval;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant trialStart;
    private Instant trialEnd;
    private Boolean cancelAtPeriodEnd;
    private Instant canceledAt;
    private Instant createdAt;
    private Map<String, String> metadata;
}
