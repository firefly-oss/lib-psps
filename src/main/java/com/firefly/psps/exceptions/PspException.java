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
 * Base exception for all PSP-related errors.
 */
public class PspException extends RuntimeException {

    private final String providerName;
    private final String errorCode;

    public PspException(String message) {
        super(message);
        this.providerName = null;
        this.errorCode = null;
    }

    public PspException(String message, Throwable cause) {
        super(message, cause);
        this.providerName = null;
        this.errorCode = null;
    }

    public PspException(String message, String providerName, String errorCode) {
        super(message);
        this.providerName = providerName;
        this.errorCode = errorCode;
    }

    public PspException(String message, String providerName, String errorCode, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.errorCode = errorCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
