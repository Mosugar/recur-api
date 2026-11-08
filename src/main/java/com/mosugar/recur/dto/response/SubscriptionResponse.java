package com.mosugar.recur.dto.response;

import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID            id,
        String          name,
        long            priceCents,
        Currency        currency,
        BillingCycle    cycle,
        LocalDate       firstPaymentDate,
        LocalDate       nextPaymentDate,
        Category        category,
        String          color,
        String          notes,
        boolean         reminderEnabled,
        boolean         active,
        OffsetDateTime  createdAt,
        OffsetDateTime  updatedAt
) {}
