package com.firefly.psps.dtos.subscriptions;
import lombok.*;
import java.util.Map;
@Data @Builder public class UpdateSubscriptionRequest {
    private String subscriptionId;
    private String planId;
    private Integer quantity;
    private String paymentMethodId;
    private Boolean cancelAtPeriodEnd;
    private Map<String, String> metadata;
}
