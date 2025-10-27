package com.firefly.psps.dtos.subscriptions;
import lombok.*;
@Data @Builder public class UpdatePricingPlanRequest {
    private String planId;
    private String name;
    private String description;
    private Boolean active;
}
