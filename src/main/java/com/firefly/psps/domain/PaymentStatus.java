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

package com.firefly.psps.domain;

/**
 * Standard payment lifecycle status.
 */
public enum PaymentStatus {
    /**
     * Payment has been created but not yet processed.
     */
    PENDING,

    /**
     * Payment requires additional action (e.g., 3DS authentication).
     */
    REQUIRES_ACTION,

    /**
     * Payment is being processed.
     */
    PROCESSING,

    /**
     * Payment has been successfully completed.
     */
    SUCCEEDED,

    /**
     * Payment has failed.
     */
    FAILED,

    /**
     * Payment has been cancelled.
     */
    CANCELLED,

    /**
     * Payment has been partially refunded.
     */
    PARTIALLY_REFUNDED,

    /**
     * Payment has been fully refunded.
     */
    REFUNDED,

    /**
     * Payment is under dispute/chargeback.
     */
    DISPUTED
}
