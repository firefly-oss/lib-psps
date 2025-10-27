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

package com.firefly.psps.resilience;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Service decorator that applies resilience patterns and observability to PSP operations.
 * 
 * This component wraps PSP operations with:
 * - Circuit Breaker: Prevents cascading failures
 * - Rate Limiter: Controls API call rate
 * - Retry: Automatic retry with exponential backoff
 * - Bulkhead: Limits concurrent calls
 * - Time Limiter: Timeout protection
 * - Metrics: Records operation timing and success/failure rates
 * 
 * Usage:
 * <pre>
 * resilientService.execute("stripe-payment", "createPayment",
 *     () -> pspAdapter.payments().createPayment(request));
 * </pre>
 */
@Component
public class ResilientPspService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientPspService.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final MeterRegistry meterRegistry;

    public ResilientPspService(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Execute a PSP operation with full resilience and observability.
     * 
     * @param providerName PSP provider name (e.g., "stripe", "adyen")
     * @param operationType Type of operation (e.g., "payment", "refund")
     * @param operation The actual PSP operation to execute
     * @param <T> Return type
     * @return Mono with resilience patterns applied
     */
    public <T> Mono<T> execute(String providerName, String operationType, Supplier<Mono<T>> operation) {
        String instanceName = providerName + "-" + operationType;
        
        // Create timer for metrics
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return operation.get()
                // Apply resilience patterns in order
                .transformDeferred(CircuitBreakerOperator.of(
                        circuitBreakerRegistry.circuitBreaker(instanceName)))
                .transformDeferred(RateLimiterOperator.of(
                        rateLimiterRegistry.rateLimiter(instanceName)))
                .transformDeferred(BulkheadOperator.of(
                        bulkheadRegistry.bulkhead(instanceName)))
                .transformDeferred(RetryOperator.of(
                        retryRegistry.retry(instanceName)))
                .transformDeferred(TimeLimiterOperator.of(
                        timeLimiterRegistry.timeLimiter(instanceName)))
                // Record metrics
                .doOnSuccess(result -> {
                    sample.stop(Timer.builder("psp.operation")
                            .tag("provider", providerName)
                            .tag("operation", operationType)
                            .tag("status", "success")
                            .register(meterRegistry));
                    
                    meterRegistry.counter("psp.operation.count",
                            "provider", providerName,
                            "operation", operationType,
                            "status", "success").increment();
                    
                    logger.debug("PSP operation successful: {}.{}", providerName, operationType);
                })
                .doOnError(error -> {
                    sample.stop(Timer.builder("psp.operation")
                            .tag("provider", providerName)
                            .tag("operation", operationType)
                            .tag("status", "failure")
                            .tag("error", error.getClass().getSimpleName())
                            .register(meterRegistry));
                    
                    meterRegistry.counter("psp.operation.count",
                            "provider", providerName,
                            "operation", operationType,
                            "status", "failure",
                            "error", error.getClass().getSimpleName()).increment();
                    
                    logger.error("PSP operation failed: {}.{} - {}", 
                            providerName, operationType, error.getMessage());
                });
    }

    /**
     * Execute a PSP operation with custom instance name for resilience.
     * Useful when you want fine-grained control over resilience instances.
     */
    public <T> Mono<T> executeWithInstanceName(
            String instanceName, 
            String providerName, 
            String operationType, 
            Supplier<Mono<T>> operation) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return operation.get()
                .transformDeferred(CircuitBreakerOperator.of(
                        circuitBreakerRegistry.circuitBreaker(instanceName)))
                .transformDeferred(RateLimiterOperator.of(
                        rateLimiterRegistry.rateLimiter(instanceName)))
                .transformDeferred(BulkheadOperator.of(
                        bulkheadRegistry.bulkhead(instanceName)))
                .transformDeferred(RetryOperator.of(
                        retryRegistry.retry(instanceName)))
                .transformDeferred(TimeLimiterOperator.of(
                        timeLimiterRegistry.timeLimiter(instanceName)))
                .doOnSuccess(result -> {
                    sample.stop(Timer.builder("psp.operation")
                            .tag("provider", providerName)
                            .tag("operation", operationType)
                            .tag("instance", instanceName)
                            .tag("status", "success")
                            .register(meterRegistry));
                })
                .doOnError(error -> {
                    sample.stop(Timer.builder("psp.operation")
                            .tag("provider", providerName)
                            .tag("operation", operationType)
                            .tag("instance", instanceName)
                            .tag("status", "failure")
                            .tag("error", error.getClass().getSimpleName())
                            .register(meterRegistry));
                });
    }
}
