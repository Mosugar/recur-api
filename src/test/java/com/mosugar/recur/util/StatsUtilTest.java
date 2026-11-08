package com.mosugar.recur.util;

import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsUtilTest {

    private static Subscription sub(long priceCents, BillingCycle cycle,
                                     Category category, Currency currency) {
        return Subscription.builder()
                .priceCents(priceCents)
                .cycle(cycle)
                .category(category)
                .currency(currency)
                .active(true)
                .firstPaymentDate(LocalDate.of(2024, 1, 1))
                .name("test")
                .build();
    }

    // ── summary ────────────────────────────────────────────────────────────────

    @Test
    void summary_allInMainCurrency() {
        // 999 monthly EUR + 1200 yearly EUR
        // Monthly total: 999 + round(1200/12)=100 = 1099
        List<Subscription> subs = List.of(
                sub(999L, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR),
                sub(1200L, BillingCycle.YEARLY, Category.MUSIC, Currency.EUR)
        );

        StatsUtil.Summary result = StatsUtil.summary(subs, Currency.EUR);

        assertThat(result.monthlyTotalCents()).isEqualTo(1099L);
        assertThat(result.yearlyTotalCents()).isEqualTo(1099L * 12);
        assertThat(result.activeCount()).isEqualTo(2);
        assertThat(result.excludedCount()).isEqualTo(0);
    }

    @Test
    void summary_mixedCurrencies_excludesNonMain() {
        List<Subscription> subs = List.of(
                sub(999L, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR),
                sub(999L, BillingCycle.MONTHLY, Category.MUSIC, Currency.USD)
        );

        StatsUtil.Summary result = StatsUtil.summary(subs, Currency.EUR);

        assertThat(result.monthlyTotalCents()).isEqualTo(999L);
        assertThat(result.activeCount()).isEqualTo(2);
        assertThat(result.excludedCount()).isEqualTo(1);
    }

    @Test
    void summary_empty_returnsZeros() {
        StatsUtil.Summary result = StatsUtil.summary(List.of(), Currency.EUR);
        assertThat(result.monthlyTotalCents()).isZero();
        assertThat(result.yearlyTotalCents()).isZero();
        assertThat(result.activeCount()).isZero();
        assertThat(result.excludedCount()).isZero();
    }

    // ── byCategory ─────────────────────────────────────────────────────────────

    @Test
    void byCategory_aggregatesCorrectly() {
        List<Subscription> subs = List.of(
                sub(1000L, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR),
                sub(500L, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR),
                sub(500L, BillingCycle.MONTHLY, Category.MUSIC, Currency.EUR)
        );

        List<StatsUtil.CategoryBreakdown> result =
                StatsUtil.byCategory(subs, Currency.EUR);

        // STREAMING: 1500, MUSIC: 500 → total 2000
        assertThat(result).hasSize(2);

        StatsUtil.CategoryBreakdown streaming = result.stream()
                .filter(b -> b.category() == Category.STREAMING).findFirst().orElseThrow();
        assertThat(streaming.monthlyAmountCents()).isEqualTo(1500L);
        assertThat(streaming.percentage()).isEqualTo(75.0);

        StatsUtil.CategoryBreakdown music = result.stream()
                .filter(b -> b.category() == Category.MUSIC).findFirst().orElseThrow();
        assertThat(music.monthlyAmountCents()).isEqualTo(500L);
        assertThat(music.percentage()).isEqualTo(25.0);
    }

    @Test
    void byCategory_excludesOtherCurrencies() {
        List<Subscription> subs = List.of(
                sub(1000L, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR),
                sub(1000L, BillingCycle.MONTHLY, Category.MUSIC, Currency.USD)
        );

        List<StatsUtil.CategoryBreakdown> result =
                StatsUtil.byCategory(subs, Currency.EUR);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(Category.STREAMING);
        assertThat(result.get(0).percentage()).isEqualTo(100.0);
    }

    @Test
    void byCategory_sortedByAmountDescending() {
        List<Subscription> subs = List.of(
                sub(100L, BillingCycle.MONTHLY, Category.MUSIC, Currency.EUR),
                sub(500L, BillingCycle.MONTHLY, Category.STREAMING, Currency.EUR),
                sub(200L, BillingCycle.MONTHLY, Category.SOFTWARE, Currency.EUR)
        );

        List<StatsUtil.CategoryBreakdown> result =
                StatsUtil.byCategory(subs, Currency.EUR);

        assertThat(result.get(0).category()).isEqualTo(Category.STREAMING);
        assertThat(result.get(1).category()).isEqualTo(Category.SOFTWARE);
        assertThat(result.get(2).category()).isEqualTo(Category.MUSIC);
    }

    @Test
    void byCategory_empty_returnsEmptyList() {
        assertThat(StatsUtil.byCategory(List.of(), Currency.EUR)).isEmpty();
    }
}
