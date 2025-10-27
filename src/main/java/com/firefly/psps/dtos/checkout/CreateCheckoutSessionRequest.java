package com.firefly.psps.dtos.checkout;
import com.firefly.psps.domain.*;
import lombok.*;
import java.util.*;
@Data @Builder public class CreateCheckoutSessionRequest {
    private CheckoutMode mode;
    private Money amount;
    private String customerId;
    private CustomerInfo customerInfo;
    private String subscriptionPlanId;
    private List<CheckoutLineItem> lineItems;
    private String successUrl;
    private String cancelUrl;
    private Long expiresAt;
    private Map<String, String> metadata;
    private List<PaymentMethodType> allowedPaymentMethods;
}
