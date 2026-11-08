package com.mosugar.recur.service.impl;

import com.mosugar.recur.dto.response.CategoryStatsResponse;
import com.mosugar.recur.dto.response.StatsSummaryResponse;
import com.mosugar.recur.dto.response.UpcomingPaymentResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.StatsService;
import com.mosugar.recur.util.BillingUtil;
import com.mosugar.recur.util.StatsUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StatsServiceImpl implements StatsService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public StatsServiceImpl(SubscriptionRepository subscriptionRepository,
                            UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<UpcomingPaymentResponse> upcoming(UUID userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(days);

        return subscriptionRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(s -> {
                    LocalDate next = BillingUtil.nextPaymentDate(
                            s.getFirstPaymentDate(), s.getCycle(), today);
                    return new UpcomingPaymentResponse(
                            s.getId(), s.getName(), s.getPriceCents(), s.getCurrency(),
                            s.getCycle(), s.getCategory(), s.getColor(),
                            next, ChronoUnit.DAYS.between(today, next));
                })
                .filter(r -> !r.nextPaymentDate().isAfter(until))
                .sorted(Comparator.comparing(UpcomingPaymentResponse::nextPaymentDate))
                .toList();
    }

    @Override
    public StatsSummaryResponse summary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Subscription> active = subscriptionRepository.findAllByUserIdAndActiveTrue(userId);
        StatsUtil.Summary s = StatsUtil.summary(active, user.getMainCurrency());
        return new StatsSummaryResponse(
                s.monthlyTotalCents(), s.yearlyTotalCents(),
                s.activeCount(), s.excludedCount());
    }

    @Override
    public List<CategoryStatsResponse> byCategory(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Subscription> active = subscriptionRepository.findAllByUserIdAndActiveTrue(userId);
        return StatsUtil.byCategory(active, user.getMainCurrency()).stream()
                .map(b -> new CategoryStatsResponse(
                        b.category(), b.monthlyAmountCents(), b.percentage()))
                .toList();
    }
}
