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

package com.firefly.psps.multitenancy;

import java.util.Map;

/**
 * Tenant context for PSP operations.
 * 
 * Allows different tenants to have their own PSP configurations:
 * - Different API credentials per tenant
 * - Tenant-specific settings (e.g., fees, limits)
 * - Isolated data and operations
 * 
 * Usage in reactive flows with Reactor Context:
 * <pre>
 * return pspAdapter.payments().createPayment(request)
 *     .contextWrite(Context.of(PspTenantContext.TENANT_ID_KEY, "tenant-123"));
 * </pre>
 */
public class PspTenantContext {

    public static final String TENANT_ID_KEY = "psp.tenant.id";
    public static final String TENANT_CONFIG_KEY = "psp.tenant.config";

    private final String tenantId;
    private final Map<String, Object> tenantConfig;

    public PspTenantContext(String tenantId) {
        this(tenantId, Map.of());
    }

    public PspTenantContext(String tenantId, Map<String, Object> tenantConfig) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }
        this.tenantId = tenantId;
        this.tenantConfig = tenantConfig != null ? Map.copyOf(tenantConfig) : Map.of();
    }

    /**
     * Get the tenant ID.
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Get tenant-specific configuration.
     */
    public Map<String, Object> getTenantConfig() {
        return tenantConfig;
    }

    /**
     * Get a specific configuration value for this tenant.
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(String key, Class<T> type) {
        Object value = tenantConfig.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("Config value for key '" + key + 
                    "' is not of type " + type.getName());
        }
        return (T) value;
    }

    /**
     * Get a configuration value with default.
     */
    public <T> T getConfigOrDefault(String key, T defaultValue) {
        Object value = tenantConfig.get(key);
        if (value == null) {
            return defaultValue;
        }
        @SuppressWarnings("unchecked")
        T result = (T) value;
        return result;
    }

    @Override
    public String toString() {
        return "PspTenantContext{" +
                "tenantId='" + tenantId + '\'' +
                ", configKeys=" + tenantConfig.keySet() +
                '}';
    }

    /**
     * Create a builder for tenant context.
     */
    public static Builder builder(String tenantId) {
        return new Builder(tenantId);
    }

    public static class Builder {
        private final String tenantId;
        private final Map<String, Object> config = new java.util.HashMap<>();

        private Builder(String tenantId) {
            this.tenantId = tenantId;
        }

        public Builder withConfig(String key, Object value) {
            this.config.put(key, value);
            return this;
        }

        public Builder withConfig(Map<String, Object> config) {
            this.config.putAll(config);
            return this;
        }

        public PspTenantContext build() {
            return new PspTenantContext(tenantId, config);
        }
    }
}
