package com.mosugar.recur.service;

import com.mosugar.recur.dto.response.CategoryStatsResponse;
import com.mosugar.recur.dto.response.StatsSummaryResponse;
import com.mosugar.recur.dto.response.UpcomingPaymentResponse;

import java.util.List;
import java.util.UUID;

public interface StatsService {

    List<UpcomingPaymentResponse> upcoming(UUID userId, int days);

    StatsSummaryResponse summary(UUID userId);

    List<CategoryStatsResponse> byCategory(UUID userId);
}
