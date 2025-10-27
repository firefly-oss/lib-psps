package com.firefly.psps.dtos.checkout;
import com.firefly.psps.domain.*;
import lombok.*;
import java.util.List;
import java.util.Map;
@Data @Builder public class CreatePaymentIntentRequest {
    private Money amount;
    private String customerId;
    private String paymentMethodId;
    private String description;
    private Boolean captureImmediately;
    private String returnUrl;
    private Map<String, String> metadata;
    private List<PaymentMethodType> allowedPaymentMethods;
}
