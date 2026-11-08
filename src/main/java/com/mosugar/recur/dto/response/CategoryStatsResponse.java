package com.mosugar.recur.dto.response;

import com.mosugar.recur.enums.Category;

public record CategoryStatsResponse(
        Category category,
        long     monthlyAmountCents,
        double   percentage
) {}
