package com.firefly.psps.dtos.customers;
import lombok.*;
@Data @Builder public class UpdateCustomerRequest {
    private String customerId;
    private String email;
    private String name;
}
