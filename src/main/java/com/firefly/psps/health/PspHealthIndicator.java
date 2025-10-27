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

package com.firefly.psps.health;

import com.firefly.psps.adapter.PspAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for PSP connectivity and circuit breaker status.
 * 
 * Provides detailed health information including:
 * - PSP adapter availability
 * - Circuit breaker states
 * - Error rates and metrics
 * 
 * Exposed via Spring Boot Actuator /actuator/health endpoint.
 */
@Component("pspHealth")
public class PspHealthIndicator implements ReactiveHealthIndicator {

    private final PspAdapter pspAdapter;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PspHealthIndicator(PspAdapter pspAdapter, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.pspAdapter = pspAdapter;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(() -> {
            Map<String, Object> details = new HashMap<>();
            
            // Check PSP adapter health
            boolean isHealthy = pspAdapter.isHealthy();
            details.put("provider", pspAdapter.getProviderName());
            details.put("available", isHealthy);
            
            // Check circuit breaker states
            Map<String, Object> circuitBreakers = new HashMap<>();
            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
                Map<String, Object> cbDetails = new HashMap<>();
                cbDetails.put("state", cb.getState().toString());
                cbDetails.put("failureRate", String.format("%.2f%%", cb.getMetrics().getFailureRate()));
                cbDetails.put("slowCallRate", String.format("%.2f%%", cb.getMetrics().getSlowCallRate()));
                cbDetails.put("numberOfCalls", cb.getMetrics().getNumberOfSuccessfulCalls() + 
                        cb.getMetrics().getNumberOfFailedCalls());
                cbDetails.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
                cbDetails.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
                
                circuitBreakers.put(cb.getName(), cbDetails);
            });
            
            details.put("circuitBreakers", circuitBreakers);
            
            // Determine overall health
            boolean allCircuitsClosed = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                    .allMatch(cb -> cb.getState() == CircuitBreaker.State.CLOSED);
            
            if (isHealthy && allCircuitsClosed) {
                return Health.up().withDetails(details).build();
            } else if (isHealthy) {
                return Health.status(new Status("DEGRADED", "Some circuit breakers are open"))
                        .withDetails(details)
                        .withDetail("reason", "Circuit breakers are not all closed").build();
            } else {
                return Health.down().withDetails(details)
                        .withDetail("reason", "PSP adapter is not healthy").build();
            }
        });
    }
}
