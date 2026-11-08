package com.mosugar.recur.dto.response;

import java.util.List;

public record SyncResponse(
        List<SubscriptionResponse> subscriptions,
        int upsertedCount
) {}
