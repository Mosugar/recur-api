package com.mosugar.recur.dto.response;

import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;

import java.time.LocalDate;
import java.util.UUID;

public record UpcomingPaymentResponse(
        UUID         id,
        String       name,
        long         priceCents,
        Currency     currency,
        BillingCycle cycle,
        Category     category,
        String       color,
        LocalDate    nextPaymentDate,
        long         daysUntilPayment
) {}
