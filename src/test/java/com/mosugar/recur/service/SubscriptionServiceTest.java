package com.mosugar.recur.service;

import com.mosugar.recur.dto.request.SubscriptionRequest;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.mapper.SubscriptionMapper;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;
    // Real MapStruct mapper: checks what is actually saved and returned
    private final SubscriptionMapper mapper = Mappers.getMapper(SubscriptionMapper.class);
    private SubscriptionServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUB_ID = UUID.randomUUID();
    private static final Sort BY_NAME = Sort.by("name");
    private User user;

    @BeforeEach
    void setUp() {
        service = new SubscriptionServiceImpl(subscriptionRepository, userRepository, mapper);
        user = User.builder().id(USER_ID).email("alice@test.com").build();
    }

    private Subscription netflix() {
        return Subscription.builder()
                .id(SUB_ID).user(user)
                .name("Netflix").priceCents(1599).currency(Currency.EUR)
                .cycle(BillingCycle.MONTHLY).firstPaymentDate(LocalDate.of(2024, 1, 15))
                .category(Category.STREAMING).color("#E50914").build();
    }

    private SubscriptionRequest request(String name, long priceCents) {
        return new SubscriptionRequest(name, priceCents, Currency.EUR, BillingCycle.YEARLY,
                LocalDate.of(2024, 3, 1), Category.SOFTWARE, "#123456", "note", true, false);
    }

    // ── list: filters and sort ───────────────────────────────────────────────

    @Test
    void list_withoutFiltersReturnsAllOfTheUsersSubscriptionsSortedByName() {
        when(subscriptionRepository.findAllByUserId(USER_ID, BY_NAME)).thenReturn(List.of(netflix()));

        List<SubscriptionResponse> result = service.list(USER_ID, null, null, null);

        assertThat(result).extracting(SubscriptionResponse::name).containsExactly("Netflix");
    }

    @Test
    void list_usesTheQueryMatchingTheGivenFilters() {
        service.list(USER_ID, true, null, null);
        verify(subscriptionRepository).findAllByUserIdAndActive(USER_ID, true, BY_NAME);

        service.list(USER_ID, null, Category.MUSIC, null);
        verify(subscriptionRepository).findAllByUserIdAndCategory(USER_ID, Category.MUSIC, BY_NAME);

        service.list(USER_ID, false, Category.MUSIC, null);
        verify(subscriptionRepository)
                .findAllByUserIdAndActiveAndCategory(USER_ID, false, Category.MUSIC, BY_NAME);
    }

    @Test
    void list_sortsByPriceDescendingAndFallsBackToNameForUnknownSorts() {
        service.list(USER_ID, null, null, "price");
        verify(subscriptionRepository)
                .findAllByUserId(USER_ID, Sort.by(Sort.Direction.DESC, "priceCents"));

        service.list(USER_ID, null, null, "anything");
        verify(subscriptionRepository).findAllByUserId(USER_ID, BY_NAME);
    }

    @Test
    void list_sortsByNextPaymentDateNotByFirstPaymentDate() {
        LocalDate today = LocalDate.now();
        // Database order is by name; first payment dates are in the opposite order of the next ones
        Subscription apple = yearly("Apple", today.plusDays(10));                    // next: +10 days
        Subscription figma = yearly("Figma", today.minusYears(3).plusDays(20));      // next: ~+20 days
        Subscription netflix = yearly("Netflix", today.minusYears(6).plusDays(1));   // next: ~+1 day
        Subscription zed = yearly("Zed", today.plusDays(10));                        // next: +10 days
        when(subscriptionRepository.findAllByUserId(USER_ID, BY_NAME))
                .thenReturn(List.of(apple, figma, netflix, zed));

        List<SubscriptionResponse> result = service.list(USER_ID, null, null, "nextPayment");

        // Soonest first; equal dates keep the name order
        assertThat(result).extracting(SubscriptionResponse::name)
                .containsExactly("Netflix", "Apple", "Zed", "Figma");
    }

    private Subscription yearly(String name, LocalDate firstPaymentDate) {
        return Subscription.builder()
                .id(UUID.randomUUID()).user(user)
                .name(name).priceCents(1000).cycle(BillingCycle.YEARLY)
                .firstPaymentDate(firstPaymentDate).build();
    }

    // ── getById ──────────────────────────────────────────────────────────────

    @Test
    void getById_returnsTheSubscriptionWithItsNextPaymentDate() {
        when(subscriptionRepository.findByIdAndUserId(SUB_ID, USER_ID)).thenReturn(Optional.of(netflix()));

        SubscriptionResponse response = service.getById(USER_ID, SUB_ID);

        assertThat(response.id()).isEqualTo(SUB_ID);
        assertThat(response.nextPaymentDate()).isAfterOrEqualTo(LocalDate.now());
        assertThat(response.nextPaymentDate().getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void getById_hidesSubscriptionsOfOtherUsers() {
        when(subscriptionRepository.findByIdAndUserId(SUB_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, SUB_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_attachesTheSubscriptionToTheUserAndSavesTheRequestFields() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.create(USER_ID, request("Figma", 14400));

        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getName()).isEqualTo("Figma");
        assertThat(saved.getValue().getCycle()).isEqualTo(BillingCycle.YEARLY);
        assertThat(saved.getValue().isActive()).isFalse();
        assertThat(response.priceCents()).isEqualTo(14400);
    }

    @Test
    void create_failsForAnUnknownUserWithoutSaving() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, request("Figma", 14400)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_appliesTheRequestButKeepsIdAndOwner() {
        Subscription existing = netflix();
        when(subscriptionRepository.findByIdAndUserId(SUB_ID, USER_ID)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);

        SubscriptionResponse response = service.update(USER_ID, SUB_ID, request("Netflix Premium", 2199));

        assertThat(existing.getId()).isEqualTo(SUB_ID);
        assertThat(existing.getUser()).isSameAs(user);
        assertThat(existing.getName()).isEqualTo("Netflix Premium");
        assertThat(existing.getPriceCents()).isEqualTo(2199);
        assertThat(existing.getCategory()).isEqualTo(Category.SOFTWARE);
        assertThat(response.name()).isEqualTo("Netflix Premium");
    }

    @Test
    void update_refusesSubscriptionsOfOtherUsersWithoutSaving() {
        when(subscriptionRepository.findByIdAndUserId(SUB_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, SUB_ID, request("Hacked", 0)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_removesAnOwnedSubscription() {
        Subscription existing = netflix();
        when(subscriptionRepository.findByIdAndUserId(SUB_ID, USER_ID)).thenReturn(Optional.of(existing));

        service.delete(USER_ID, SUB_ID);

        verify(subscriptionRepository).delete(existing);
    }

    @Test
    void delete_refusesSubscriptionsOfOtherUsersWithoutDeleting() {
        when(subscriptionRepository.findByIdAndUserId(SUB_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, SUB_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(subscriptionRepository, never()).delete(any());
    }
}
