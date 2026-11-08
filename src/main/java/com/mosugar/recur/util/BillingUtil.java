package com.mosugar.recur.util;

import com.mosugar.recur.enums.BillingCycle;

import java.time.LocalDate;

/**
 * Pure, stateless billing helpers.
 * All methods are static so they can be tested without Spring context.
 */
public final class BillingUtil {

    private BillingUtil() {}

    /**
     * Compute the next payment date on or after {@code today}.
     *
     * <p>Steps forward by one cycle period at a time from {@code firstPaymentDate}
     * until the result is strictly after today - 1 (i.e., >= today).
     * Uses {@link LocalDate#plusMonths} / {@link LocalDate#plusWeeks} /
     * {@link LocalDate#plusYears} so month-end clamping is handled by the JDK
     * (e.g. Jan 31 + 1 month = Feb 28/29).
     *
     * @param firstPaymentDate the anchor date for the billing schedule
     * @param cycle            billing frequency
     * @param today            reference date (usually LocalDate.now())
     * @return next due date >= today
     */
    public static LocalDate nextPaymentDate(
            LocalDate firstPaymentDate, BillingCycle cycle, LocalDate today) {

        LocalDate next = firstPaymentDate;

        // Fast-forward: if firstPaymentDate is in the future, it IS the next date.
        if (!next.isBefore(today)) {
            return next;
        }

        // Advance in full cycle steps until next >= today.
        while (next.isBefore(today)) {
            next = addOneCycle(next, cycle);
        }
        return next;
    }

    /**
     * Monthly-equivalent amount in cents (rounded to nearest cent).
     *
     * <ul>
     *   <li>WEEKLY    → × 52 / 12</li>
     *   <li>MONTHLY   → × 1</li>
     *   <li>QUARTERLY → / 3</li>
     *   <li>YEARLY    → / 12</li>
     * </ul>
     */
    public static long monthlyEquivalentCents(long priceCents, BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY    -> Math.round(priceCents * 52.0 / 12.0);
            case MONTHLY   -> priceCents;
            case QUARTERLY -> Math.round(priceCents / 3.0);
            case YEARLY    -> Math.round(priceCents / 12.0);
        };
    }

    /**
     * Yearly-equivalent amount in cents (= monthlyEquivalent × 12).
     */
    public static long yearlyEquivalentCents(long priceCents, BillingCycle cycle) {
        return monthlyEquivalentCents(priceCents, cycle) * 12;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static LocalDate addOneCycle(LocalDate date, BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY    -> date.plusWeeks(1);
            case MONTHLY   -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY    -> date.plusYears(1);
        };
    }
}
