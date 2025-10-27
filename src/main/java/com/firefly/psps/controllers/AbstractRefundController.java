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

import com.firefly.psps.dtos.refunds.*;
import com.firefly.psps.services.AbstractPspService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Abstract REST controller for refund operations.
 * 
 * Implementations can extend this to get standardized refund endpoints.
 * Override methods to customize behavior or add additional endpoints.
 * 
 * Default endpoints:
 * - POST   /refunds - Create refund
 * - GET    /refunds/{id} - Get refund details
 */
public abstract class AbstractRefundController {

    protected final AbstractPspService pspService;

    protected AbstractRefundController(AbstractPspService pspService) {
        this.pspService = pspService;
    }

    /**
     * Create a new refund for a payment.
     * 
     * POST /api/refunds
     */
    @PostMapping
    public Mono<ResponseEntity<RefundResponse>> createRefund(@Valid @RequestBody CreateRefundRequest request) {
        return pspService.createRefund(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get refund details by ID.
     * 
     * GET /api/refunds/{refundId}
     */
    @GetMapping("/{refundId}")
    public Mono<ResponseEntity<RefundResponse>> getRefund(@PathVariable String refundId) {
        return pspService.getRefund(refundId)
                .map(ResponseEntity::ok);
    }
}
