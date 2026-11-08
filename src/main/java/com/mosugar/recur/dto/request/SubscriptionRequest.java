package com.mosugar.recur.dto.request;

import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record SubscriptionRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotNull
        @Min(0)
        Long priceCents,

        @NotNull
        Currency currency,

        @NotNull
        BillingCycle cycle,

        @NotNull
        LocalDate firstPaymentDate,

        @NotNull
        Category category,

        @NotBlank
        @Pattern(regexp = "^#([0-9A-Fa-f]{6})$", message = "must be a hex color like #RRGGBB")
        String color,

        String notes,

        boolean reminderEnabled,

        boolean active
) {}
