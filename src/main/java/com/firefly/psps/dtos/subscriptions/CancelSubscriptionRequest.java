package com.firefly.psps.dtos.subscriptions;
import lombok.*;
@Data @Builder public class CancelSubscriptionRequest {
    private String subscriptionId;
    private Boolean immediately;
    private String cancellationReason;
}
