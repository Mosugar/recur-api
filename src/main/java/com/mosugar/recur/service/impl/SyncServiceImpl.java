package com.mosugar.recur.service.impl;

import com.mosugar.recur.dto.request.SyncSubscriptionItem;
import com.mosugar.recur.dto.request.SyncRequest;
import com.mosugar.recur.dto.response.SyncResponse;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.mapper.SubscriptionMapper;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.SyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyncServiceImpl implements SyncService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper subscriptionMapper;

    public SyncServiceImpl(SubscriptionRepository subscriptionRepository,
                           UserRepository userRepository,
                           SubscriptionMapper subscriptionMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    @Transactional
    public SyncResponse sync(UUID userId, SyncRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<UUID> incomingIds = request.subscriptions().stream()
                .map(SyncSubscriptionItem::id).toList();

        // Load all existing records that match the incoming IDs in one query (no N+1)
        Map<UUID, Subscription> existing = subscriptionRepository
                .findAllByUserIdAndIdIn(userId, incomingIds).stream()
                .collect(Collectors.toMap(Subscription::getId, Function.identity()));

        List<Subscription> toSave = new ArrayList<>();
        int upsertedCount = 0;

        for (SyncSubscriptionItem item : request.subscriptions()) {
            Subscription sub = existing.get(item.id());

            if (sub == null) {
                // New subscription from the app
                sub = buildFromItem(item, user);
                toSave.add(sub);
                upsertedCount++;
            } else if (item.updatedAt().isAfter(sub.getUpdatedAt())) {
                // App version is newer — overwrite (last-write-wins)
                applyItem(item, sub);
                toSave.add(sub);
                upsertedCount++;
            }
            // Otherwise server version is newer or equal — skip
        }

        subscriptionRepository.saveAll(toSave);

        // Return full server state for the user
        List<SubscriptionResponse> serverState = subscriptionRepository
                .findAllByUserIdAndActiveTrue(userId).stream()
                .map(subscriptionMapper::toResponse)
                .toList();

        return new SyncResponse(serverState, upsertedCount);
    }

    private Subscription buildFromItem(SyncSubscriptionItem item, User user) {
        return Subscription.builder()
                .id(item.id())
                .user(user)
                .name(item.name())
                .priceCents(item.priceCents())
                .currency(item.currency())
                .cycle(item.cycle())
                .firstPaymentDate(item.firstPaymentDate())
                .category(item.category())
                .color(item.color())
                .notes(item.notes())
                .reminderEnabled(item.reminderEnabled())
                .active(item.active())
                .build();
    }

    private void applyItem(SyncSubscriptionItem item, Subscription sub) {
        sub.setName(item.name());
        sub.setPriceCents(item.priceCents());
        sub.setCurrency(item.currency());
        sub.setCycle(item.cycle());
        sub.setFirstPaymentDate(item.firstPaymentDate());
        sub.setCategory(item.category());
        sub.setColor(item.color());
        sub.setNotes(item.notes());
        sub.setReminderEnabled(item.reminderEnabled());
        sub.setActive(item.active());
    }
}
