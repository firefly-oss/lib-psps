package com.firefly.psps.dtos.payouts;
import lombok.*;
@Data @Builder public class PayoutScheduleResponse {
    private String interval;
    private Integer delayDays;
}
