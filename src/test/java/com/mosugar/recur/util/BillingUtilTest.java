package com.mosugar.recur.util;

import com.mosugar.recur.enums.BillingCycle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static com.mosugar.recur.enums.BillingCycle.*;
import static org.assertj.core.api.Assertions.assertThat;

class BillingUtilTest {

    // ── nextPaymentDate ────────────────────────────────────────────────────────

    @Test
    void nextPaymentDate_firstPaymentIsToday_returnsToday() {
        LocalDate today = LocalDate.of(2024, 3, 15);
        assertThat(BillingUtil.nextPaymentDate(today, MONTHLY, today)).isEqualTo(today);
    }

    @Test
    void nextPaymentDate_firstPaymentIsFuture_returnsThatDate() {
        LocalDate first = LocalDate.of(2024, 5, 1);
        LocalDate today = LocalDate.of(2024, 3, 15);
        assertThat(BillingUtil.nextPaymentDate(first, MONTHLY, today)).isEqualTo(first);
    }

    @Test
    void nextPaymentDate_monthly_advancesCorrectly() {
        LocalDate first = LocalDate.of(2024, 1, 10);
        LocalDate today = LocalDate.of(2024, 3, 15);
        // Jan10 → Feb10 → Mar10 (past) → Apr10
        assertThat(BillingUtil.nextPaymentDate(first, MONTHLY, today))
                .isEqualTo(LocalDate.of(2024, 4, 10));
    }

    @Test
    void nextPaymentDate_weekly_advancesCorrectly() {
        LocalDate first = LocalDate.of(2024, 1, 1);  // Monday
        LocalDate today = LocalDate.of(2024, 1, 15);
        // 1→8→15: 15 >= today → returns Jan 15
        assertThat(BillingUtil.nextPaymentDate(first, WEEKLY, today))
                .isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void nextPaymentDate_quarterly_advancesCorrectly() {
        LocalDate first = LocalDate.of(2024, 1, 1);
        LocalDate today = LocalDate.of(2024, 5, 1);
        // Jan1 → Apr1 (past) → Jul1
        assertThat(BillingUtil.nextPaymentDate(first, QUARTERLY, today))
                .isEqualTo(LocalDate.of(2024, 7, 1));
    }

    @Test
    void nextPaymentDate_yearly_advancesCorrectly() {
        LocalDate first = LocalDate.of(2022, 6, 15);
        LocalDate today = LocalDate.of(2024, 7, 1);
        // 2022 → 2023 → 2024 (past Jun 15) → 2025
        assertThat(BillingUtil.nextPaymentDate(first, YEARLY, today))
                .isEqualTo(LocalDate.of(2025, 6, 15));
    }

    // ── Month-end edge cases ───────────────────────────────────────────────────

    @Test
    void nextPaymentDate_jan31PlusOneMonth_clampsToFeb28() {
        LocalDate first = LocalDate.of(2023, 1, 31);
        LocalDate today = LocalDate.of(2023, 2, 28);
        // Jan31 + 1 month = Feb28 (2023 is not a leap year) >= today
        assertThat(BillingUtil.nextPaymentDate(first, MONTHLY, today))
                .isEqualTo(LocalDate.of(2023, 2, 28));
    }

    @Test
    void nextPaymentDate_jan31PlusOneMonth_leapYear_clampsToFeb29() {
        LocalDate first = LocalDate.of(2024, 1, 31);
        LocalDate today = LocalDate.of(2024, 2, 1);
        // 2024 is a leap year: Jan31 + 1 month = Feb29 >= today
        assertThat(BillingUtil.nextPaymentDate(first, MONTHLY, today))
                .isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void nextPaymentDate_mar31PlusOneMonth_clampsToApr30() {
        LocalDate first = LocalDate.of(2024, 3, 31);
        LocalDate today = LocalDate.of(2024, 4, 1);
        // Mar31 + 1 month = Apr30 >= today
        assertThat(BillingUtil.nextPaymentDate(first, MONTHLY, today))
                .isEqualTo(LocalDate.of(2024, 4, 30));
    }

    @Test
    void nextPaymentDate_monthEndChain_clampsConsistently() {
        // Jan31 + 1m = Feb29 (leap 2024); Feb29 + 1m = Mar29 (JDK: same day clamped);
        // Mar29 + 1m = Apr29 >= Apr 1 → returns Apr29.
        // Note: once clamped to day 29, subsequent months stay at 29 unless shorter.
        LocalDate first = LocalDate.of(2024, 1, 31);
        LocalDate today = LocalDate.of(2024, 4, 1);
        assertThat(BillingUtil.nextPaymentDate(first, MONTHLY, today))
                .isEqualTo(LocalDate.of(2024, 4, 29));
    }

    // ── monthlyEquivalentCents ─────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} @ {1} cents -> {2} cents/month")
    @CsvSource({
        "WEEKLY,   100, 433",   // round(100 * 52 / 12) = round(433.33) = 433
        "MONTHLY,  999, 999",
        "QUARTERLY,900, 300",   // 900/3 = 300
        "YEARLY,   1200, 100",  // 1200/12 = 100
        // Rounding: weekly 1 cent -> round(1*52/12) = round(4.33) = 4
        "WEEKLY,   1,   4",
        // Rounding: quarterly 10 cents -> round(10/3) = round(3.33) = 3
        "QUARTERLY,10,  3",
        // Rounding: yearly 10 cents -> round(10/12) = round(0.83) = 1
        "YEARLY,   10,  1"
    })
    void monthlyEquivalentCents(BillingCycle cycle, long price, long expected) {
        assertThat(BillingUtil.monthlyEquivalentCents(price, cycle)).isEqualTo(expected);
    }

    // ── yearlyEquivalentCents ──────────────────────────────────────────────────

    @Test
    void yearlyEquivalentCents_isMonthlyTimesX12() {
        long price = 999L;
        for (BillingCycle cycle : BillingCycle.values()) {
            long yearly = BillingUtil.yearlyEquivalentCents(price, cycle);
            long expectedMonthly = BillingUtil.monthlyEquivalentCents(price, cycle);
            assertThat(yearly).isEqualTo(expectedMonthly * 12);
        }
    }
}
