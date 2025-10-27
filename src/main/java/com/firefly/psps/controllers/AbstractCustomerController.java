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

package com.firefly.psps.controllers;

import com.firefly.psps.dtos.customers.*;
import com.firefly.psps.services.AbstractPspService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Abstract REST controller for customer management operations.
 * 
 * Implementations can extend this to get standardized customer endpoints.
 * Override methods to customize behavior or add additional endpoints.
 * 
 * Default endpoints:
 * - POST   /customers - Create customer
 * - GET    /customers/{id} - Get customer details
 * - PATCH  /customers/{id} - Update customer
 * - DELETE /customers/{id} - Delete customer
 * - POST   /customers/{id}/payment-methods - Attach payment method
 * - GET    /customers/{id}/payment-methods - List payment methods
 * - DELETE /customers/{id}/payment-methods/{pmId} - Detach payment method
 */
public abstract class AbstractCustomerController {

    protected final AbstractPspService pspService;

    protected AbstractCustomerController(AbstractPspService pspService) {
        this.pspService = pspService;
    }

    /**
     * Create a new customer.
     * 
     * POST /api/customers
     */
    @PostMapping
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        return pspService.createCustomer(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get customer details by ID.
     * 
     * GET /api/customers/{customerId}
     */
    @GetMapping("/{customerId}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(@PathVariable String customerId) {
        return pspService.getCustomer(customerId)
                .map(ResponseEntity::ok);
    }

    /**
     * Update customer information.
     * 
     * PATCH /api/customers/{customerId}
     */
    @PatchMapping("/{customerId}")
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        // Set customer ID from path parameter if not already set
        if (request.getCustomerId() == null) {
            request.setCustomerId(customerId);
        }
        return pspService.updateCustomer(request)
                .map(ResponseEntity::ok);
    }

    /**
     * Delete a customer.
     * 
     * DELETE /api/customers/{customerId}
     */
    @DeleteMapping("/{customerId}")
    public Mono<ResponseEntity<Void>> deleteCustomer(@PathVariable String customerId) {
        return pspService.deleteCustomer(customerId)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    /**
     * Attach a payment method to a customer.
     * 
     * POST /api/customers/{customerId}/payment-methods
     */
    @PostMapping("/{customerId}/payment-methods")
    public Mono<ResponseEntity<PaymentMethodResponse>> attachPaymentMethod(
            @PathVariable String customerId,
            @Valid @RequestBody AttachPaymentMethodRequest request) {
        // Set customer ID from path parameter if not already set
        if (request.getCustomerId() == null) {
            request.setCustomerId(customerId);
        }
        return pspService.attachPaymentMethod(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * List payment methods for a customer.
     * 
     * GET /api/customers/{customerId}/payment-methods
     */
    @GetMapping("/{customerId}/payment-methods")
    public Mono<ResponseEntity<java.util.List<PaymentMethodResponse>>> listPaymentMethods(
            @PathVariable String customerId) {
        return pspService.listPaymentMethods(customerId)
                .map(ResponseEntity::ok);
    }

    /**
     * Detach a payment method from a customer.
     * 
     * DELETE /api/customers/{customerId}/payment-methods/{paymentMethodId}
     */
    @DeleteMapping("/{customerId}/payment-methods/{paymentMethodId}")
    public Mono<ResponseEntity<Void>> detachPaymentMethod(
            @PathVariable String customerId,
            @PathVariable String paymentMethodId) {
        return pspService.detachPaymentMethod(customerId, paymentMethodId)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
