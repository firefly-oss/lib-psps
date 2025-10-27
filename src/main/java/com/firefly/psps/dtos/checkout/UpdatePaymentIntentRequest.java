package com.firefly.psps.dtos.checkout;
import com.firefly.psps.domain.Money;
import lombok.*;
import java.util.Map;
@Data @Builder public class UpdatePaymentIntentRequest {
    private String intentId;
    private Money amount;
    private String description;
    private Map<String, String> metadata;
}
