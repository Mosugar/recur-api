package com.mosugar.recur.service;

import com.mosugar.recur.dto.request.SyncRequest;
import com.mosugar.recur.dto.request.SyncSubscriptionItem;
import com.mosugar.recur.dto.response.SyncResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.mapper.SubscriptionMapper;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.impl.SyncServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionMapper subscriptionMapper;
    @InjectMocks SyncServiceImpl syncService;

    private static final UUID USER_ID = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email("alice@test.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of());
        when(subscriptionMapper.toResponse(any())).thenReturn(null);
    }

    private SyncSubscriptionItem item(UUID id, OffsetDateTime updatedAt) {
        return new SyncSubscriptionItem(
                id, "Netflix", 999L, Currency.EUR, BillingCycle.MONTHLY,
                LocalDate.of(2024, 1, 1), Category.STREAMING,
                "#6366F1", null, false, true, updatedAt);
    }

    @Test
    void sync_newItem_isInserted() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findAllByUserIdAndIdIn(any(), any()))
                .thenReturn(List.of());

        SyncResponse response = syncService.sync(USER_ID, new SyncRequest(
                List.of(item(id, OffsetDateTime.now()))));

        assertThat(response.upsertedCount()).isEqualTo(1);
        verify(subscriptionRepository).saveAll(argThat(list ->
                ((List<?>) list).size() == 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_serverNewerThanApp_isSkipped() {
        UUID id = UUID.randomUUID();
        OffsetDateTime serverTime = OffsetDateTime.now();
        OffsetDateTime appTime    = serverTime.minusHours(1); // app is older

        Subscription existing = Subscription.builder()
                .id(id).user(user).updatedAt(serverTime).build();
        when(subscriptionRepository.findAllByUserIdAndIdIn(any(), any()))
                .thenReturn(List.of(existing));

        SyncResponse response = syncService.sync(USER_ID, new SyncRequest(
                List.of(item(id, appTime))));

        assertThat(response.upsertedCount()).isEqualTo(0);
        // saveAll should still be called but with empty list
        ArgumentCaptor<List<Subscription>> captor = ArgumentCaptor.forClass(List.class);
        verify(subscriptionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_appNewerThanServer_isUpdated() {
        UUID id = UUID.randomUUID();
        OffsetDateTime serverTime = OffsetDateTime.now().minusHours(1);
        OffsetDateTime appTime    = OffsetDateTime.now();

        Subscription existing = Subscription.builder()
                .id(id).user(user)
                .name("OldName").priceCents(100L).currency(Currency.EUR)
                .cycle(BillingCycle.MONTHLY).firstPaymentDate(LocalDate.now())
                .category(Category.OTHER).color("#000000").active(true)
                .updatedAt(serverTime).build();
        when(subscriptionRepository.findAllByUserIdAndIdIn(any(), any()))
                .thenReturn(List.of(existing));

        syncService.sync(USER_ID, new SyncRequest(List.of(item(id, appTime))));

        ArgumentCaptor<List<Subscription>> captor = ArgumentCaptor.forClass(List.class);
        verify(subscriptionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        // Name should be updated to "Netflix" from the item
        assertThat(captor.getValue().get(0).getName()).isEqualTo("Netflix");
    }

    @Test
    void sync_returnsFullServerState() {
        when(subscriptionRepository.findAllByUserIdAndIdIn(any(), any()))
                .thenReturn(List.of());
        // Server has 2 active subs
        Subscription s1 = Subscription.builder().id(UUID.randomUUID()).user(user).build();
        Subscription s2 = Subscription.builder().id(UUID.randomUUID()).user(user).build();
        when(subscriptionRepository.findAllByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(s1, s2));

        SyncResponse response = syncService.sync(USER_ID, new SyncRequest(List.of()));

        // Response contains 2 items (server state)
        assertThat(response.subscriptions()).hasSize(2);
    }
}
