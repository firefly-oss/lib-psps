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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Abstract base implementation for provider-specific operations.
 * <p>
 * Provides a registry pattern for registering and executing provider-specific operations.
 * Similar to the DataEnrichers pattern in lib-common-data.
 * <p>
 * Example usage:
 * <pre>{@code
 * public class StripeProviderSpecificPort extends AbstractProviderSpecificPort {
 *     public StripeProviderSpecificPort() {
 *         registerOperation("create_connect_account", this::createConnectAccount);
 *         registerOperation("manage_radar_rules", this::manageRadarRules);
 *     }
 *     
 *     private Mono<ResponseEntity<ProviderOperationResponse>> createConnectAccount(
 *             ProviderOperationRequest req) {
 *         // Implementation
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractProviderSpecificPort implements ProviderSpecificPort {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final Map<String, ProviderOperation> operations = new ConcurrentHashMap<>();
    private final Map<String, OperationMetadata> metadata = new ConcurrentHashMap<>();

    /**
     * Register a provider-specific operation.
     *
     * @param operationName unique name for the operation
     * @param handler function that handles the operation
     */
    protected void registerOperation(
            String operationName,
            Function<ProviderOperationRequest, Mono<ResponseEntity<ProviderOperationResponse>>> handler) {
        operations.put(operationName, new ProviderOperation(operationName, handler));
        logger.info("Registered provider-specific operation: {}", operationName);
    }

    /**
     * Register a provider-specific operation with metadata.
     *
     * @param operationName unique name for the operation
     * @param handler function that handles the operation
     * @param metadata operation metadata (description, parameters, etc.)
     */
    protected void registerOperation(
            String operationName,
            Function<ProviderOperationRequest, Mono<ResponseEntity<ProviderOperationResponse>>> handler,
            OperationMetadata metadata) {
        registerOperation(operationName, handler);
        this.metadata.put(operationName, metadata);
    }

    @Override
    public Mono<ResponseEntity<ProviderOperationResponse>> executeOperation(
            String operationName,
            ProviderOperationRequest request) {
        
        ProviderOperation operation = operations.get(operationName);
        
        if (operation == null) {
            return Mono.error(new UnsupportedProviderOperationException(operationName, getProviderName()));
        }

        logger.info("Executing provider-specific operation: {}", operationName);
        
        return operation.handler.apply(request)
                .doOnSuccess(response -> logger.info("Operation '{}' completed successfully", operationName))
                .doOnError(error -> logger.error("Operation '{}' failed", operationName, error));
    }

    @Override
    public boolean supportsOperation(String operationName) {
        return operations.containsKey(operationName);
    }

    @Override
    public Set<String> getSupportedOperations() {
        return new HashSet<>(operations.keySet());
    }

    @Override
    public Mono<ResponseEntity<ProviderOperationResponse>> getOperationMetadata(String operationName) {
        OperationMetadata meta = metadata.get(operationName);
        
        if (meta == null) {
            return Mono.error(new UnsupportedProviderOperationException(operationName, getProviderName()));
        }

        ProviderOperationResponse response = ProviderOperationResponse.builder()
                .operationName(operationName)
                .success(true)
                .result(Map.of(
                        "description", meta.description,
                        "parameters", meta.parameters,
                        "example", meta.example
                ))
                .build();

        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Get the provider name (should be implemented by subclass).
     *
     * @return the provider name
     */
    protected abstract String getProviderName();

    /**
     * Internal class to hold operation handler.
     */
    private static class ProviderOperation {
        final String name;
        final Function<ProviderOperationRequest, Mono<ResponseEntity<ProviderOperationResponse>>> handler;

        ProviderOperation(
                String name,
                Function<ProviderOperationRequest, Mono<ResponseEntity<ProviderOperationResponse>>> handler) {
            this.name = name;
            this.handler = handler;
        }
    }

    /**
     * Metadata for a provider-specific operation.
     */
    @lombok.Data
    @lombok.Builder
    public static class OperationMetadata {
        private String description;
        private Map<String, String> parameters;
        private Map<String, Object> example;
    }
}
