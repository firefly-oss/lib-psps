package com.firefly.psps.dtos.subscriptions;
import com.firefly.psps.domain.SubscriptionStatus;
import lombok.*;
@Data @Builder public class ListSubscriptionsRequest {
    private SubscriptionStatus status;
    private Integer limit;
    private String startingAfter;
}
