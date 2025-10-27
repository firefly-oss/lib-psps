package com.firefly.psps.dtos.payments;
import lombok.*;
import java.util.Map;
@Data @Builder public class UpdatePaymentRequest {
    private String paymentId;
    private String description;
    private Map<String, String> metadata;
}
