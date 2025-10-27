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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Value object representing a monetary amount with currency.
 * Immutable and thread-safe.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Money {

    @NotNull
    @Positive
    private final BigDecimal amount;

    @NotNull
    private final Currency currency;

    @JsonCreator
    public Money(
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("currency") Currency currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency must not be null");
        }
        this.amount = amount;
        this.currency = currency;
    }

    /**
     * Creates a Money instance from the smallest currency unit (e.g., cents for USD).
     *
     * @param amountInCents the amount in the smallest unit
     * @param currency the currency
     * @return Money instance
     */
    public static Money fromCents(long amountInCents, Currency currency) {
        return new Money(BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100)), currency);
    }

    /**
     * Converts the amount to the smallest currency unit (e.g., cents).
     *
     * @return amount in cents
     */
    public long toCents() {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
