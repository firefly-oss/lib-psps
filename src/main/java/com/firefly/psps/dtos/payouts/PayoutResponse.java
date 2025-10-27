package com.firefly.psps.dtos.payouts;
import com.firefly.psps.domain.Money;
import lombok.*;
import java.time.Instant;
@Data @Builder public class PayoutResponse {
    private String payoutId;
    private Money amount;
    private String status;
    private Instant createdAt;
}
