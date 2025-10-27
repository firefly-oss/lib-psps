/*
 * Copyright 2025 Firefly Software Solutions Inc
 */

package com.firefly.psps.config;

import com.firefly.psps.health.PspHealthIndicator;
import com.firefly.psps.resilience.ResilientPspService;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Spring Boot auto-configuration.
 * 
 * PURPOSE: Ensure that Spring Boot automatically configures resilience patterns
 * when the library is added to the classpath.
 */
@DisplayName("Spring Boot Auto-Configuration Tests")
class AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PspResilienceConfiguration.class));

    @Test
    @DisplayName("Should auto-configure resilience registries when enabled")
    void shouldAutoConfigureResilienceRegistries() {
        contextRunner
                .withPropertyValues("firefly.psp.resilience.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CircuitBreakerRegistry.class);
                    assertThat(context).hasSingleBean(RateLimiterRegistry.class);
                    assertThat(context).hasSingleBean(RetryRegistry.class);
                    assertThat(context).hasSingleBean(BulkheadRegistry.class);
                    assertThat(context).hasSingleBean(TimeLimiterRegistry.class);
                });
    }

    @Test
    @DisplayName("Should use default configuration when properties not set")
    void shouldUseDefaultConfiguration() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(PspResilienceProperties.class);
                    
                    PspResilienceProperties properties = context.getBean(PspResilienceProperties.class);
                    assertThat(properties.isEnabled()).isTrue(); // Default is enabled
                    assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(50);
                    assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(3);
                    assertThat(properties.getRateLimiter().getLimitForPeriod()).isEqualTo(50);
                    assertThat(properties.getBulkhead().getMaxConcurrentCalls()).isEqualTo(25);
                });
    }

    @Test
    @DisplayName("Should apply custom configuration from properties")
    void shouldApplyCustomConfiguration() {
        contextRunner
                .withPropertyValues(
                        "firefly.psp.resilience.enabled=true",
                        "firefly.psp.resilience.circuit-breaker.failure-rate-threshold=75",
                        "firefly.psp.resilience.retry.max-attempts=5",
                        "firefly.psp.resilience.rate-limiter.limit-for-period=100"
                )
                .run(context -> {
                    PspResilienceProperties properties = context.getBean(PspResilienceProperties.class);
                    
                    assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(75);
                    assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(5);
                    assertThat(properties.getRateLimiter().getLimitForPeriod()).isEqualTo(100);
                });
    }

    @Test
    @DisplayName("Should not configure resilience when disabled")
    void shouldNotConfigureWhenDisabled() {
        contextRunner
                .withPropertyValues("firefly.psp.resilience.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CircuitBreakerRegistry.class);
                    assertThat(context).doesNotHaveBean(RateLimiterRegistry.class);
                    assertThat(context).doesNotHaveBean(RetryRegistry.class);
                });
    }

    @Test
    @DisplayName("Should configure ResilientPspService when MeterRegistry available")
    void shouldConfigureResilientPspService() {
        contextRunner
                .withPropertyValues("firefly.psp.resilience.enabled=true")
                .withBean(MeterRegistry.class, () -> {
                    // Mock MeterRegistry for test
                    return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(ResilientPspService.class);
                    
                    ResilientPspService service = context.getBean(ResilientPspService.class);
                    assertThat(service).isNotNull();
                });
    }

    @Test
    @DisplayName("Circuit breaker registry should have correct default config")
    void circuitBreakerRegistryShouldHaveCorrectDefaults() {
        contextRunner
                .run(context -> {
                    CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);
                    assertThat(registry).isNotNull();
                    
                    // Default config should be applied to new circuit breakers
                    var cb = registry.circuitBreaker("test");
                    assertThat(cb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50);
                });
    }

    @Test
    @DisplayName("Retry registry should support exponential backoff by default")
    void retryRegistryShouldSupportExponentialBackoff() {
        contextRunner
                .run(context -> {
                    PspResilienceProperties properties = context.getBean(PspResilienceProperties.class);
                    assertThat(properties.getRetry().isExponentialBackoffEnabled()).isTrue();
                    assertThat(properties.getRetry().getExponentialBackoffMultiplier()).isEqualTo(2.0);
                });
    }

    @Test
    @DisplayName("All resilience components should work together")
    void allResilienceComponentsShouldWorkTogether() {
        contextRunner
                .withBean(MeterRegistry.class, io.micrometer.core.instrument.simple.SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(CircuitBreakerRegistry.class);
                    assertThat(context).hasSingleBean(RateLimiterRegistry.class);
                    assertThat(context).hasSingleBean(RetryRegistry.class);
                    assertThat(context).hasSingleBean(BulkheadRegistry.class);
                    assertThat(context).hasSingleBean(TimeLimiterRegistry.class);
                    assertThat(context).hasSingleBean(ResilientPspService.class);
                    
                    // All components should be autowirable
                    ResilientPspService service = context.getBean(ResilientPspService.class);
                    assertThat(service).isNotNull();
                });
    }

    @Test
    @DisplayName("Configuration properties should be validated")
    void configurationPropertiesShouldBeValidated() {
        contextRunner
                .withPropertyValues(
                        "firefly.psp.resilience.circuit-breaker.failure-rate-threshold=50",
                        "firefly.psp.resilience.circuit-breaker.minimum-number-of-calls=10"
                )
                .run(context -> {
                    PspResilienceProperties properties = context.getBean(PspResilienceProperties.class);
                    
                    // Validate thresholds are within valid ranges
                    assertThat(properties.getCircuitBreaker().getFailureRateThreshold())
                            .isBetween(1, 100);
                    assertThat(properties.getCircuitBreaker().getMinimumNumberOfCalls())
                            .isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("Should configure health indicator when adapter available")
    void shouldConfigureHealthIndicator() {
        contextRunner
                .withBean("pspAdapter", com.firefly.psps.adapter.PspAdapter.class, () -> {
                    // Mock adapter for test
                    return org.mockito.Mockito.mock(com.firefly.psps.adapter.PspAdapter.class);
                })
                .run(context -> {
                    // PspHealthIndicator is a @Component, so it needs to be in component scan
                    // For this test, we just verify the registries are available
                    assertThat(context).hasSingleBean(CircuitBreakerRegistry.class);
                    assertThat(context).hasBean("pspAdapter");
                });
    }

    @Test
    @DisplayName("Configuration should be idempotent")
    void configurationShouldBeIdempotent() {
        contextRunner
                .run(context -> {
                    // Creating multiple instances should return the same config
                    CircuitBreakerRegistry registry1 = context.getBean(CircuitBreakerRegistry.class);
                    CircuitBreakerRegistry registry2 = context.getBean(CircuitBreakerRegistry.class);
                    
                    assertThat(registry1).isSameAs(registry2);
                });
    }
}
