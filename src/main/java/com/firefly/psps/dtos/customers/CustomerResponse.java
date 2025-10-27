package com.firefly.psps.dtos.customers;
import lombok.*;
import java.time.Instant;
@Data @Builder public class CustomerResponse {
    private String customerId;
    private String email;
    private String name;
    private Instant createdAt;
}
