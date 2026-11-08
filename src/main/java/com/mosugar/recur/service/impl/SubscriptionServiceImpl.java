package com.mosugar.recur.service.impl;

import com.mosugar.recur.dto.request.SubscriptionRequest;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.mapper.SubscriptionMapper;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.SubscriptionService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   UserRepository userRepository,
                                   SubscriptionMapper subscriptionMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public List<SubscriptionResponse> list(UUID userId,
                                           Boolean active,
                                           Category category,
                                           String sort) {
        Sort jpaSort = resolveSort(sort);
        List<Subscription> results;

        if (active != null && category != null) {
            results = subscriptionRepository.findAllByUserIdAndActiveAndCategory(
                    userId, active, category, jpaSort);
        } else if (active != null) {
            results = subscriptionRepository.findAllByUserIdAndActive(userId, active, jpaSort);
        } else if (category != null) {
            results = subscriptionRepository.findAllByUserIdAndCategory(userId, category, jpaSort);
        } else {
            results = subscriptionRepository.findAllByUserId(userId, jpaSort);
        }

        return results.stream().map(subscriptionMapper::toResponse).toList();
    }

    @Override
    public SubscriptionResponse getById(UUID userId, UUID id) {
        Subscription sub = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        return subscriptionMapper.toResponse(sub);
    }

    @Override
    @Transactional
    public SubscriptionResponse create(UUID userId, SubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Subscription sub = subscriptionMapper.toEntity(request);
        sub.setUser(user);
        return subscriptionMapper.toResponse(subscriptionRepository.save(sub));
    }

    @Override
    @Transactional
    public SubscriptionResponse update(UUID userId, UUID id, SubscriptionRequest request) {
        Subscription sub = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        subscriptionMapper.updateEntity(request, sub);
        return subscriptionMapper.toResponse(subscriptionRepository.save(sub));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        Subscription sub = subscriptionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        subscriptionRepository.delete(sub);
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by("name");
        return switch (sort) {
            case "price"       -> Sort.by(Sort.Direction.DESC, "priceCents");
            case "nextPayment" -> Sort.by("firstPaymentDate");
            default            -> Sort.by("name");
        };
    }
}
