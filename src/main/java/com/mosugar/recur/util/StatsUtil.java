package com.mosugar.recur.util;

import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregation helpers for the /stats endpoints.
 * Operates on a list of active subscriptions already loaded from the DB.
 */
public final class StatsUtil {

    private StatsUtil() {}

    public record Summary(
            long monthlyTotalCents,
            long yearlyTotalCents,
            int  activeCount,
            int  excludedCount
    ) {}

    public record CategoryBreakdown(
            Category category,
            long     monthlyAmountCents,
            double   percentage          // 0–100, rounded to 2 dp
    ) {}

    /**
     * Compute totals for active subscriptions in the user's main currency.
     * Subscriptions in other currencies are counted but excluded from amounts.
     */
    public static Summary summary(List<Subscription> activeSubscriptions,
                                  Currency mainCurrency) {
        long monthlyTotal = 0;
        int excluded = 0;

        for (Subscription s : activeSubscriptions) {
            if (s.getCurrency() == mainCurrency) {
                monthlyTotal += BillingUtil.monthlyEquivalentCents(s.getPriceCents(), s.getCycle());
            } else {
                excluded++;
            }
        }

        return new Summary(
                monthlyTotal,
                monthlyTotal * 12,
                activeSubscriptions.size(),
                excluded
        );
    }

    /**
     * Per-category monthly totals and percentage share (of total monthly spend
     * in main currency). Categories with zero spend are omitted.
     */
    public static List<CategoryBreakdown> byCategory(
            List<Subscription> activeSubscriptions, Currency mainCurrency) {

        Map<Category, Long> totals = new EnumMap<>(Category.class);

        for (Subscription s : activeSubscriptions) {
            if (s.getCurrency() != mainCurrency) continue;
            long monthly = BillingUtil.monthlyEquivalentCents(s.getPriceCents(), s.getCycle());
            totals.merge(s.getCategory(), monthly, Long::sum);
        }

        long grandTotal = totals.values().stream().mapToLong(Long::longValue).sum();

        return totals.entrySet().stream()
                .map(e -> new CategoryBreakdown(
                        e.getKey(),
                        e.getValue(),
                        grandTotal == 0 ? 0.0
                                : Math.round(e.getValue() * 10000.0 / grandTotal) / 100.0))
                .sorted((a, b) -> Long.compare(b.monthlyAmountCents(), a.monthlyAmountCents()))
                .toList();
    }
}
