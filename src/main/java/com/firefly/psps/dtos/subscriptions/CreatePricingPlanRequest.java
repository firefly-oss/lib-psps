package com.firefly.psps.dtos.subscriptions;
import com.firefly.psps.domain.*;
import lombok.*;
@Data @Builder public class CreatePricingPlanRequest {
    private String name;
    private String description;
    private Money amount;
    private BillingInterval interval;
    private Integer intervalCount;
    private Integer trialPeriodDays;
}
