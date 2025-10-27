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

package com.firefly.psps.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for PSP resilience patterns.
 * 
 * Creates and configures Resilience4j registries for:
 * - Circuit Breakers: Prevent cascading failures
 * - Rate Limiters: Control API call rate per PSP
 * - Retries: Automatic retry with exponential backoff
 * - Bulkheads: Limit concurrent calls
 * - Time Limiters: Timeout protection
 */
@Configuration
@EnableConfigurationProperties(PspResilienceProperties.class)
@ConditionalOnProperty(prefix = "firefly.psp.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PspResilienceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PspResilienceConfiguration.class);

    /**
     * Creates a CircuitBreakerRegistry with PSP-specific configuration.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(PspResilienceProperties properties) {
        var cbConfig = properties.getCircuitBreaker();
        
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureRateThreshold())
                .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                .waitDurationInOpenState(cbConfig.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowSize(cbConfig.getSlidingWindowSize())
                .slowCallDurationThreshold(cbConfig.getSlowCallDurationThreshold())
                .slowCallRateThreshold(cbConfig.getSlowCallRateThreshold())
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        // Log circuit breaker state changes
        registry.getEventPublisher().onEntryAdded(event -> {
            CircuitBreaker cb = event.getAddedEntry();
            cb.getEventPublisher()
                    .onStateTransition(e -> logger.warn("PSP CircuitBreaker '{}' state changed: {} -> {}", 
                            cb.getName(), e.getStateTransition().getFromState(), e.getStateTransition().getToState()))
                    .onError(e -> logger.error("PSP CircuitBreaker '{}' recorded error: {}", 
                            cb.getName(), e.getThrowable().getMessage()))
                    .onSlowCallRateExceeded(e -> logger.warn("PSP CircuitBreaker '{}' slow call rate exceeded: {}%", 
                            cb.getName(), e.getSlowCallRate()))
                    .onFailureRateExceeded(e -> logger.error("PSP CircuitBreaker '{}' failure rate exceeded: {}%", 
                            cb.getName(), e.getFailureRate()));
        });

        logger.info("PSP CircuitBreaker registry configured: failureRate={}%, minCalls={}, waitDuration={}", 
                cbConfig.getFailureRateThreshold(), cbConfig.getMinimumNumberOfCalls(), cbConfig.getWaitDurationInOpenState());

        return registry;
    }

    /**
     * Creates a RateLimiterRegistry with PSP-specific configuration.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry(PspResilienceProperties properties) {
        var rlConfig = properties.getRateLimiter();
        
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(rlConfig.getLimitRefreshPeriod())
                .limitForPeriod(rlConfig.getLimitForPeriod())
                .timeoutDuration(rlConfig.getTimeoutDuration())
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        
        // Log rate limiter events
        registry.getEventPublisher().onEntryAdded(event -> {
            RateLimiter rl = event.getAddedEntry();
            rl.getEventPublisher()
                    .onFailure(e -> logger.warn("PSP RateLimiter '{}' permission denied", rl.getName()));
        });

        logger.info("PSP RateLimiter registry configured: limit={}/period={}", 
                rlConfig.getLimitForPeriod(), rlConfig.getLimitRefreshPeriod());

        return registry;
    }

    /**
     * Creates a RetryRegistry with PSP-specific configuration.
     */
    @Bean
    public RetryRegistry retryRegistry(PspResilienceProperties properties) {
        var retryConfig = properties.getRetry();
        
        RetryConfig.Builder<Object> configBuilder = RetryConfig.custom()
                .maxAttempts(retryConfig.getMaxAttempts());

        if (retryConfig.isExponentialBackoffEnabled()) {
            configBuilder.intervalFunction(io.github.resilience4j.core.IntervalFunction
                    .ofExponentialBackoff(
                            retryConfig.getWaitDuration().toMillis(),
                            retryConfig.getExponentialBackoffMultiplier(),
                            retryConfig.getExponentialMaxWaitDuration().toMillis()
                    ));
        } else {
            configBuilder.waitDuration(retryConfig.getWaitDuration());
        }

        RetryConfig config = configBuilder.build();
        RetryRegistry registry = RetryRegistry.of(config);
        
        // Log retry events
        registry.getEventPublisher().onEntryAdded(event -> {
            Retry retry = event.getAddedEntry();
            retry.getEventPublisher()
                    .onRetry(e -> logger.debug("PSP Retry '{}' attempt {}", 
                            retry.getName(), e.getNumberOfRetryAttempts()))
                    .onError(e -> logger.error("PSP Retry '{}' exhausted after {} attempts", 
                            retry.getName(), e.getNumberOfRetryAttempts()));
        });

        logger.info("PSP Retry registry configured: maxAttempts={}, waitDuration={}, exponentialBackoff={}", 
                retryConfig.getMaxAttempts(), retryConfig.getWaitDuration(), retryConfig.isExponentialBackoffEnabled());

        return registry;
    }

    /**
     * Creates a BulkheadRegistry with PSP-specific configuration.
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry(PspResilienceProperties properties) {
        var bhConfig = properties.getBulkhead();
        
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(bhConfig.getMaxConcurrentCalls())
                .maxWaitDuration(bhConfig.getMaxWaitDuration())
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        
        // Log bulkhead events
        registry.getEventPublisher().onEntryAdded(event -> {
            Bulkhead bulkhead = event.getAddedEntry();
            bulkhead.getEventPublisher()
                    .onCallRejected(e -> logger.warn("PSP Bulkhead '{}' rejected call (full)", bulkhead.getName()))
                    .onCallFinished(e -> logger.debug("PSP Bulkhead '{}' call finished, available: {}", 
                            bulkhead.getName(), bulkhead.getMetrics().getAvailableConcurrentCalls()));
        });

        logger.info("PSP Bulkhead registry configured: maxConcurrentCalls={}", bhConfig.getMaxConcurrentCalls());

        return registry;
    }

    /**
     * Creates a TimeLimiterRegistry with PSP-specific configuration.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry(PspResilienceProperties properties) {
        var tlConfig = properties.getTimeLimiter();
        
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(tlConfig.getTimeoutDuration())
                .cancelRunningFuture(tlConfig.isCancelRunningFuture())
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);

        logger.info("PSP TimeLimiter registry configured: timeout={}", tlConfig.getTimeoutDuration());

        return registry;
    }

    /**
     * Creates ResilientPspService bean for applying resilience patterns.
     * Only created when MeterRegistry is available.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(io.micrometer.core.instrument.MeterRegistry.class)
    public com.firefly.psps.resilience.ResilientPspService resilientPspService(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        
        logger.info("PSP ResilientPspService configured");
        return new com.firefly.psps.resilience.ResilientPspService(
                circuitBreakerRegistry,
                rateLimiterRegistry,
                retryRegistry,
                bulkheadRegistry,
                timeLimiterRegistry,
                meterRegistry
        );
    }
}
