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

import com.firefly.psps.dtos.disputes.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port interface for dispute management operations.
 * 
 * Handles chargebacks and dispute resolution processes.
 */
public interface DisputePort {

    /**
     * Retrieve a dispute by its identifier.
     *
     * @param disputeId PSP-specific dispute identifier
     * @return reactive publisher with dispute details
     */
    Mono<ResponseEntity<DisputeResponse>> getDispute(String disputeId);

    /**
     * List disputes with optional filtering.
     *
     * @param request list request with pagination and filters
     * @return reactive publisher with list of disputes
     */
    Mono<ResponseEntity<List<DisputeResponse>>> listDisputes(ListDisputesRequest request);

    /**
     * Submit evidence for a dispute.
     *
     * @param request evidence submission request
     * @return reactive publisher with updated dispute response
     */
    Mono<ResponseEntity<DisputeResponse>> submitEvidence(SubmitEvidenceRequest request);

    /**
     * Accept a dispute (do not contest it).
     *
     * @param disputeId dispute identifier to accept
     * @return reactive publisher with updated dispute response
     */
    Mono<ResponseEntity<DisputeResponse>> acceptDispute(String disputeId);

    /**
     * Close a dispute.
     *
     * @param disputeId dispute identifier to close
     * @return reactive publisher with closed dispute response
     */
    Mono<ResponseEntity<DisputeResponse>> closeDispute(String disputeId);
}
