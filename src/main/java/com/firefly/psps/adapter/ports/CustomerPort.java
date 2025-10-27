/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.psps.adapter.ports;

import com.firefly.psps.dtos.customers.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port interface for customer management operations.
 * 
 * Handles customer creation, updates, and payment method management.
 */
public interface CustomerPort {

    /**
     * Create a customer in the PSP.
     *
     * @param request customer creation request
     * @return reactive publisher with customer response
     */
    Mono<ResponseEntity<CustomerResponse>> createCustomer(CreateCustomerRequest request);

    /**
     * Retrieve a customer by identifier.
     *
     * @param customerId PSP-specific customer identifier
     * @return reactive publisher with customer details
     */
    Mono<ResponseEntity<CustomerResponse>> getCustomer(String customerId);

    /**
     * Update customer information.
     *
     * @param request customer update request
     * @return reactive publisher with updated customer response
     */
    Mono<ResponseEntity<CustomerResponse>> updateCustomer(UpdateCustomerRequest request);

    /**
     * Delete a customer from the PSP.
     *
     * @param customerId customer identifier to delete
     * @return reactive publisher indicating completion
     */
    Mono<Void> deleteCustomer(String customerId);

    /**
     * List customers with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of customers
     */
    Mono<ResponseEntity<List<CustomerResponse>>> listCustomers(ListCustomersRequest request);

    /**
     * Attach a payment method to a customer.
     *
     * @param request payment method attachment request
     * @return reactive publisher with payment method response
     */
    Mono<ResponseEntity<PaymentMethodResponse>> attachPaymentMethod(AttachPaymentMethodRequest request);

    /**
     * Detach a payment method from a customer.
     *
     * @param customerId customer identifier
     * @param paymentMethodId payment method identifier
     * @return reactive publisher indicating completion
     */
    Mono<Void> detachPaymentMethod(String customerId, String paymentMethodId);

    /**
     * List payment methods for a customer.
     *
     * @param customerId customer identifier
     * @return reactive publisher with list of payment methods
     */
    Mono<ResponseEntity<List<PaymentMethodResponse>>> listPaymentMethods(String customerId);

    /**
     * Set a payment method as the default for a customer.
     *
     * @param customerId customer identifier
     * @param paymentMethodId payment method identifier to set as default
     * @return reactive publisher with updated customer response
     */
    Mono<ResponseEntity<CustomerResponse>> setDefaultPaymentMethod(String customerId, String paymentMethodId);
}
