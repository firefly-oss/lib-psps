package com.firefly.psps.dtos.checkout;
import com.firefly.psps.domain.Money;
import lombok.*;
@Data @Builder public class CheckoutLineItem {
    private String name;
    private String description;
    private Money price;
    private Integer quantity;
    private String planId;
}
