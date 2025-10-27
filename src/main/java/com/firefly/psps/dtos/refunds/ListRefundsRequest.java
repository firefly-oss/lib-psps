package com.firefly.psps.dtos.refunds;
import lombok.*;
@Data @Builder public class ListRefundsRequest {
    private Integer limit;
    private String startingAfter;
}
