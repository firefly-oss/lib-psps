package com.firefly.psps.dtos.subscriptions;
import com.firefly.psps.domain.Money;
import lombok.*;
@Data @Builder public class InvoiceLineItem {
    private String description;
    private Integer quantity;
    private Money amount;
    private Money totalAmount;
}
