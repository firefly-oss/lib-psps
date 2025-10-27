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

import com.firefly.psps.validation.PaymentValidator;

import java.util.List;

/**
 * Exception thrown when payment validation fails.
 * 
 * Contains the validation errors that prevented the request from being sent to PSP.
 */
public class PaymentValidationException extends PspException {

    private final List<PaymentValidator.ValidationError> validationErrors;

    public PaymentValidationException(String message, List<PaymentValidator.ValidationError> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public PaymentValidationException(String message, List<PaymentValidator.ValidationError> validationErrors, Throwable cause) {
        super(message, cause);
        this.validationErrors = validationErrors;
    }

    public List<PaymentValidator.ValidationError> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (validationErrors != null && !validationErrors.isEmpty()) {
            sb.append(": ");
            validationErrors.forEach(error -> 
                sb.append(String.format("[%s: %s] ", error.field(), error.message()))
            );
        }
        return sb.toString();
    }
}
