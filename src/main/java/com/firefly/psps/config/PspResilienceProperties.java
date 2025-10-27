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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for PSP resilience patterns.
 * 
 * Configures circuit breakers, rate limiters, retries, bulkheads and timeouts
 * for PSP operations to ensure fault tolerance and prevent cascading failures.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "firefly.psp.resilience")
public class PspResilienceProperties {

    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private RateLimiterConfig rateLimiter = new RateLimiterConfig();
    private RetryConfig retry = new RetryConfig();
    private BulkheadConfig bulkhead = new BulkheadConfig();
    private TimeLimiterConfig timeLimiter = new TimeLimiterConfig();
    private boolean enabled = true;

    @Data
    public static class CircuitBreakerConfig {
        /**
         * Failure rate threshold percentage (0-100) to open circuit.
         * Default: 50%
         */
        private int failureRateThreshold = 50;

        /**
         * Minimum number of calls before circuit breaker calculates error rate.
         * Default: 10
         */
        private int minimumNumberOfCalls = 10;

        /**
         * Time to wait in open state before transitioning to half-open.
         * Default: 60 seconds
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);

        /**
         * Number of calls in half-open state to determine if circuit should close.
         * Default: 5
         */
        private int permittedNumberOfCallsInHalfOpenState = 5;

        /**
         * Sliding window size for failure rate calculation.
         * Default: 100
         */
        private int slidingWindowSize = 100;

        /**
         * Slow call duration threshold to consider a call as slow.
         * Default: 10 seconds
         */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(10);

        /**
         * Slow call rate threshold percentage to open circuit.
         * Default: 100% (disabled)
         */
        private int slowCallRateThreshold = 100;
    }

    @Data
    public static class RateLimiterConfig {
        /**
         * Period of limit refresh.
         * Default: 1 second
         */
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);

        /**
         * Number of permissions available during refresh period.
         * Default: 50 calls per second
         */
        private int limitForPeriod = 50;

        /**
         * Time a thread waits for permission.
         * Default: 5 seconds
         */
        private Duration timeoutDuration = Duration.ofSeconds(5);
    }

    @Data
    public static class RetryConfig {
        /**
         * Maximum number of retry attempts.
         * Default: 3
         */
        private int maxAttempts = 3;

        /**
         * Wait duration between retry attempts.
         * Default: 1 second
         */
        private Duration waitDuration = Duration.ofSeconds(1);

        /**
         * Multiplier for exponential backoff.
         * Default: 2
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * Maximum wait duration for exponential backoff.
         * Default: 10 seconds
         */
        private Duration exponentialMaxWaitDuration = Duration.ofSeconds(10);

        /**
         * Enable exponential backoff.
         * Default: true
         */
        private boolean exponentialBackoffEnabled = true;
    }

    @Data
    public static class BulkheadConfig {
        /**
         * Maximum concurrent calls allowed.
         * Default: 25
         */
        private int maxConcurrentCalls = 25;

        /**
         * Maximum wait time for permission to execute.
         * Default: 500ms
         */
        private Duration maxWaitDuration = Duration.ofMillis(500);
    }

    @Data
    public static class TimeLimiterConfig {
        /**
         * Timeout duration for PSP operations.
         * Default: 30 seconds
         */
        private Duration timeoutDuration = Duration.ofSeconds(30);

        /**
         * Whether to cancel running future on timeout.
         * Default: true
         */
        private boolean cancelRunningFuture = true;
    }
}
