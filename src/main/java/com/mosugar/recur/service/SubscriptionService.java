package com.mosugar.recur.service;

import com.mosugar.recur.dto.request.SubscriptionRequest;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.enums.Category;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    List<SubscriptionResponse> list(UUID userId,
                                    Boolean active,
                                    Category category,
                                    String sort);

    SubscriptionResponse getById(UUID userId, UUID id);

    SubscriptionResponse create(UUID userId, SubscriptionRequest request);

    SubscriptionResponse update(UUID userId, UUID id, SubscriptionRequest request);

    void delete(UUID userId, UUID id);
}
