package com.firefly.psps.dtos.payouts;
import com.firefly.psps.domain.Money;
import lombok.*;
@Data @Builder public class CreatePayoutRequest {
    private Money amount;
    private String destination;
    private String description;
}
