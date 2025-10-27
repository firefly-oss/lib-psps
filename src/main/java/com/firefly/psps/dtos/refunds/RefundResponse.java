package com.firefly.psps.dtos.refunds;
import com.firefly.psps.domain.Money;
import lombok.*;
import java.time.Instant;
@Data @Builder public class RefundResponse {
    private String refundId;
    private String paymentId;
    private Money amount;
    private String status;
    private Instant createdAt;
}
