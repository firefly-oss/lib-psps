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

package com.firefly.psps.exceptions;

/**
 * Exception thrown when a provider-specific operation is not supported.
 */
public class UnsupportedProviderOperationException extends PspException {
    
    public UnsupportedProviderOperationException(String operationName) {
        super("Provider operation not supported: " + operationName);
    }
    
    public UnsupportedProviderOperationException(String operationName, String providerName) {
        super("Provider operation '" + operationName + "' not supported by provider: " + providerName);
    }
}
