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

package com.firefly.psps.routing;

import com.firefly.psps.adapter.PspAdapter;
import com.firefly.psps.domain.Currency;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Router for intelligent PSP selection.
 * 
 * Enables advanced routing strategies:
 * - Failover: Switch to backup PSP if primary is down
 * - Region-based: Route by customer location
 * - Currency-optimized: Choose PSP with best rates for currency
 * - Cost-optimized: Select PSP with lowest fees
 * - A/B testing: Split traffic between PSPs
 * - Load balancing: Distribute load across multiple PSPs
 * 
 * Example:
 * <pre>
 * PspAdapter psp = router.selectPsp(RoutingContext.builder()
 *     .currency(Currency.EUR)
 *     .region("EU")
 *     .amount(Money.of(100, EUR))
 *     .build());
 * </pre>
 */
public interface PspRouter {

    /**
     * Select the best PSP adapter based on routing context.
     *
     * @param context routing decision context
     * @return selected PSP adapter
     */
    Mono<PspAdapter> selectPsp(RoutingContext context);

    /**
     * Select PSP with failover support.
     * Returns primary PSP, but falls back to secondary if primary is unhealthy.
     *
     * @param context routing context
     * @return PSP adapter with failover
     */
    Mono<PspAdapter> selectPspWithFailover(RoutingContext context);

    /**
     * Get all available PSP adapters.
     *
     * @return list of configured PSP adapters
     */
    List<PspAdapter> getAllPsps();

    /**
     * Get PSP adapter by provider name.
     *
     * @param providerName provider name (e.g., "stripe", "adyen")
     * @return PSP adapter if found
     */
    Mono<PspAdapter> getPspByName(String providerName);

    /**
     * Check which PSPs support a given currency.
     *
     * @param currency currency to check
     * @return list of PSPs supporting this currency
     */
    List<PspAdapter> getPspsByCurrency(Currency currency);

    /**
     * Get PSP health status for all configured providers.
     *
     * @return map of provider name to health status
     */
    Map<String, Boolean> getPspHealthStatus();

    /**
     * Routing context for PSP selection.
     */
    interface RoutingContext {
        String getTenantId();
        Currency getCurrency();
        String getRegion();
        String getCustomerId();
        String getPaymentMethodType();
        Map<String, Object> getMetadata();
        
        static Builder builder() {
            return new Builder();
        }
        
        class Builder {
            private String tenantId;
            private Currency currency;
            private String region;
            private String customerId;
            private String paymentMethodType;
            private Map<String, Object> metadata = Map.of();
            
            public Builder tenantId(String tenantId) {
                this.tenantId = tenantId;
                return this;
            }
            
            public Builder currency(Currency currency) {
                this.currency = currency;
                return this;
            }
            
            public Builder region(String region) {
                this.region = region;
                return this;
            }
            
            public Builder customerId(String customerId) {
                this.customerId = customerId;
                return this;
            }
            
            public Builder paymentMethodType(String paymentMethodType) {
                this.paymentMethodType = paymentMethodType;
                return this;
            }
            
            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
                return this;
            }
            
            public RoutingContext build() {
                return new DefaultRoutingContext(
                        tenantId, currency, region, customerId, 
                        paymentMethodType, metadata);
            }
        }
    }

    /**
     * Default immutable implementation of RoutingContext.
     */
    record DefaultRoutingContext(
            String tenantId,
            Currency currency,
            String region,
            String customerId,
            String paymentMethodType,
            Map<String, Object> metadata
    ) implements RoutingContext {
        public DefaultRoutingContext {
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public Currency getCurrency() {
            return currency;
        }

        @Override
        public String getRegion() {
            return region;
        }

        @Override
        public String getCustomerId() {
            return customerId;
        }

        @Override
        public String getPaymentMethodType() {
            return paymentMethodType;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Routing strategy for PSP selection.
     */
    enum RoutingStrategy {
        /**
         * Route to first healthy PSP.
         */
        FIRST_AVAILABLE,
        
        /**
         * Route based on currency optimization.
         */
        CURRENCY_OPTIMIZED,
        
        /**
         * Route based on lowest fees.
         */
        COST_OPTIMIZED,
        
        /**
         * Round-robin load balancing.
         */
        ROUND_ROBIN,
        
        /**
         * Random selection.
         */
        RANDOM,
        
        /**
         * Region-based routing.
         */
        REGION_BASED,
        
        /**
         * Tenant-specific routing.
         */
        TENANT_SPECIFIC,
        
        /**
         * Custom routing logic.
         */
        CUSTOM
    }
}
