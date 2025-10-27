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

/**
 * Configuration properties for PSP integration.
 */
@Data
@ConfigurationProperties(prefix = "firefly.psp")
public class PspProperties {

    /**
     * Active PSP provider name (e.g., "stripe", "adyen").
     */
    private String provider;

    /**
     * Whether to enable PSP auto-configuration.
     */
    private boolean enabled = true;

    /**
     * API base path for PSP endpoints.
     */
    private String basePath = "/api/psp";
}
