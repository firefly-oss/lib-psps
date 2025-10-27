package com.firefly.psps.dtos.payments;
import lombok.*;
@Data @Builder public class ListPaymentsRequest {
    private Integer limit;
    private String startingAfter;
}
