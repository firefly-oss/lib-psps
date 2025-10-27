package com.firefly.psps.dtos.disputes;
import com.firefly.psps.domain.Money;
import lombok.*;
import java.time.Instant;
@Data @Builder public class DisputeResponse {
    private String disputeId;
    private String paymentId;
    private Money amount;
    private String status;
    private String reason;
    private Instant createdAt;
}
