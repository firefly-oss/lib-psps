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

import com.firefly.psps.dtos.provider.ProviderOperationRequest;
import com.firefly.psps.dtos.provider.ProviderOperationResponse;
import com.firefly.psps.exceptions.UnsupportedProviderOperationException;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Port interface for provider-specific operations.
 * 
 * This allows implementations to expose PSP-specific features that don't fit
 * into the standard port interfaces, similar to DataEnrichers in lib-common-data.
 * 
 * Examples:
 * - Stripe: Create Connect accounts, manage Radar rules
 * - Adyen: Configure split payments, manage stored payment methods
 * - PayPal: Manage billing agreements, create orders
 * 
 * This provides extensibility without polluting the core interfaces.
 */
public interface ProviderSpecificPort {

    /**
     * Execute a provider-specific operation.
     * 
     * @param operationName the name of the provider-specific operation
     * @param request the operation request with provider-specific parameters
     * @return reactive publisher with operation response
     * @throws UnsupportedProviderOperationException if operation not supported
     */
    Mono<ResponseEntity<ProviderOperationResponse>> executeOperation(
            String operationName, 
            ProviderOperationRequest request);

    /**
     * Check if a specific operation is supported by this provider.
     *
     * @param operationName the operation name to check
     * @return true if supported, false otherwise
     */
    boolean supportsOperation(String operationName);

    /**
     * Get all supported provider-specific operations.
     *
     * @return set of supported operation names
     */
    Set<String> getSupportedOperations();

    /**
     * Get metadata/documentation for a specific operation.
     *
     * @param operationName the operation name
     * @return operation metadata (parameters, description, etc.)
     */
    Mono<ResponseEntity<ProviderOperationResponse>> getOperationMetadata(String operationName);
}
