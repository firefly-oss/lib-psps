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

import com.firefly.psps.dtos.subscriptions.*;
import com.firefly.psps.services.AbstractPspService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * Abstract REST controller for subscription operations.
 * 
 * Implementations can extend this to get standardized subscription endpoints.
 */
public abstract class AbstractSubscriptionController {

    protected final AbstractPspService pspService;

    protected AbstractSubscriptionController(AbstractPspService pspService) {
        this.pspService = pspService;
    }

    /**
     * Create a new subscription.
     * 
     * POST /api/subscriptions
     */
    @PostMapping
    public Mono<ResponseEntity<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        return pspService.createSubscription(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get subscription by ID.
     * 
     * GET /api/subscriptions/{subscriptionId}
     */
    @GetMapping("/{subscriptionId}")
    public Mono<ResponseEntity<SubscriptionResponse>> getSubscription(@PathVariable String subscriptionId) {
        return pspService.getSubscription(subscriptionId)
                .map(ResponseEntity::ok);
    }

    /**
     * Cancel a subscription.
     * 
     * POST /api/subscriptions/{subscriptionId}/cancel
     */
    @PostMapping("/{subscriptionId}/cancel")
    public Mono<ResponseEntity<SubscriptionResponse>> cancelSubscription(
            @PathVariable String subscriptionId,
            @RequestBody(required = false) CancelSubscriptionRequest request) {
        
        if (request == null) {
            request = CancelSubscriptionRequest.builder()
                    .subscriptionId(subscriptionId)
                    .immediately(false)
                    .build();
        } else if (request.getSubscriptionId() == null) {
            request = CancelSubscriptionRequest.builder()
                    .subscriptionId(subscriptionId)
                    .immediately(request.getImmediately())
                    .cancellationReason(request.getCancellationReason())
                    .build();
        }
        
        return pspService.cancelSubscription(request)
                .map(ResponseEntity::ok);
    }
}
