package com.mosugar.recur.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SyncRequest(
        @NotNull
        @Valid
        List<SyncSubscriptionItem> subscriptions
) {}
