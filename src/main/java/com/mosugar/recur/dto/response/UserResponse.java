package com.mosugar.recur.dto.response;

import com.mosugar.recur.enums.Currency;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID            id,
        String          email,
        String          displayName,
        Currency        mainCurrency,
        OffsetDateTime  createdAt
) {}
