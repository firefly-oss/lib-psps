package com.firefly.psps.dtos.subscriptions;
import com.firefly.psps.domain.*;
import lombok.*;
import java.time.Instant;
@Data @Builder public class PricingPlanResponse {
    private String planId;
    private String name;
    private String description;
    private Money amount;
    private BillingInterval interval;
    private Integer intervalCount;
    private Integer trialPeriodDays;
    private Boolean active;
    private Instant createdAt;
}
