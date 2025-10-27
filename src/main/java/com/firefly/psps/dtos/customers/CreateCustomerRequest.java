package com.firefly.psps.dtos.customers;
import com.firefly.psps.domain.CustomerInfo;
import lombok.*;
@Data @Builder public class CreateCustomerRequest {
    private CustomerInfo customerInfo;
}
