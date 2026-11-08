package com.mosugar.recur.service;

import com.mosugar.recur.dto.response.CategoryStatsResponse;
import com.mosugar.recur.dto.response.StatsSummaryResponse;
import com.mosugar.recur.dto.response.UpcomingPaymentResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.impl.StatsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;
    @InjectMocks StatsServiceImpl statsService;

    private static final UUID USER_ID = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("alice@test.com")
                .mainCurrency(Currency.EUR)
                .build();
    }

    private void stubUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private Subscription sub(long price, BillingCycle cycle, Category cat, Currency curr,
                              LocalDate first) {
        return Subscription.builder()
                .id(UUID.randomUUID()).user(user)
                .name("S").priceCents(price).currency(curr)
                .cycle(cycle).firstPaymentDate(first)
                .category(cat).active(true).color("#000000").build();
    }

    // ── upcoming ──────────────────────────────────────────────────────────────

    @Test
    void upcoming_returnsPaymentsDueWithinWindow() {
        LocalDate today = LocalDate.now();
        // Due in 3 days
        Subscription s = sub(999, BillingCycle.MONTHLY, Category.STREAMING,
                Currency.EUR, today.plusDays(3));
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(s));

        List<UpcomingPaymentResponse> result = statsService.upcoming(USER_ID, 7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).daysUntilPayment()).isEqualTo(3L);
    }

    @Test
    void upcoming_excludesPaymentsBeyondWindow() {
        LocalDate today = LocalDate.now();
        Subscription s = sub(999, BillingCycle.MONTHLY, Category.STREAMING,
                Currency.EUR, today.plusDays(10));
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(s));

        assertThat(statsService.upcoming(USER_ID, 7)).isEmpty();
    }

    @Test
    void upcoming_sortsByNextPaymentDate() {
        LocalDate today = LocalDate.now();
        Subscription s1 = sub(999, BillingCycle.MONTHLY, Category.STREAMING,
                Currency.EUR, today.plusDays(5));
        Subscription s2 = sub(999, BillingCycle.MONTHLY, Category.MUSIC,
                Currency.EUR, today.plusDays(2));
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(s1, s2));

        List<UpcomingPaymentResponse> result = statsService.upcoming(USER_ID, 7);

        assertThat(result.get(0).daysUntilPayment()).isEqualTo(2L);
        assertThat(result.get(1).daysUntilPayment()).isEqualTo(5L);
    }

    // ── summary ────────────────────────────────────────────────────────────────

    @Test
    void summary_returnsTotalsForMainCurrency() {
        stubUser();
        Subscription s1 = sub(999, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR,
                LocalDate.of(2024, 1, 1));
        Subscription s2 = sub(999, BillingCycle.MONTHLY, Category.MUSIC, Currency.USD,
                LocalDate.of(2024, 1, 1));
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(s1, s2));

        StatsSummaryResponse result = statsService.summary(USER_ID);

        assertThat(result.monthlyTotalCents()).isEqualTo(999L);
        assertThat(result.yearlyTotalCents()).isEqualTo(999L * 12);
        assertThat(result.activeCount()).isEqualTo(2);
        assertThat(result.excludedCount()).isEqualTo(1);
    }

    // ── byCategory ─────────────────────────────────────────────────────────────

    @Test
    void byCategory_returnsCorrectBreakdown() {
        stubUser();
        Subscription s1 = sub(1000, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR,
                LocalDate.of(2024, 1, 1));
        Subscription s2 = sub(500, BillingCycle.MONTHLY, Category.MUSIC, Currency.EUR,
                LocalDate.of(2024, 1, 1));
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(s1, s2));

        List<CategoryStatsResponse> result = statsService.byCategory(USER_ID);

        assertThat(result).hasSize(2);
        CategoryStatsResponse top = result.get(0);
        assertThat(top.category()).isEqualTo(Category.STREAMING);
        assertThat(top.monthlyAmountCents()).isEqualTo(1000L);
        // 1000 / 1500 = 66.67%
        assertThat(top.percentage()).isEqualTo(66.67);
    }
}
