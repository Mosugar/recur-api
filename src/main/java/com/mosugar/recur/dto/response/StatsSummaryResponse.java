package com.mosugar.recur.dto.response;

public record StatsSummaryResponse(
        long monthlyTotalCents,
        long yearlyTotalCents,
        int  activeCount,
        int  excludedCount
) {}
