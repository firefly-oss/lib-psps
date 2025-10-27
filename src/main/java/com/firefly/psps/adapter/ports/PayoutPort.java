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

import com.firefly.psps.dtos.payouts.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port interface for payout operations.
 * 
 * Handles transferring funds to external bank accounts or payment methods.
 */
public interface PayoutPort {

    /**
     * Create a payout to an external account.
     *
     * @param request payout creation request
     * @return reactive publisher with payout response
     */
    Mono<ResponseEntity<PayoutResponse>> createPayout(CreatePayoutRequest request);

    /**
     * Retrieve a payout by its identifier.
     *
     * @param payoutId PSP-specific payout identifier
     * @return reactive publisher with payout details
     */
    Mono<ResponseEntity<PayoutResponse>> getPayout(String payoutId);

    /**
     * Cancel a pending payout (if supported by the PSP).
     *
     * @param payoutId payout identifier to cancel
     * @return reactive publisher with cancelled payout response
     */
    Mono<ResponseEntity<PayoutResponse>> cancelPayout(String payoutId);

    /**
     * List payouts with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of payouts
     */
    Mono<ResponseEntity<List<PayoutResponse>>> listPayouts(ListPayoutsRequest request);

    /**
     * Get the payout schedule for an account (if applicable).
     *
     * @param accountId account identifier
     * @return reactive publisher with payout schedule details
     */
    Mono<ResponseEntity<PayoutScheduleResponse>> getPayoutSchedule(String accountId);
}
