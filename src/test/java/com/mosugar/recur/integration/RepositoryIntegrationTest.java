package com.mosugar.recur.integration;

import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.repository.SubscriptionRepository;
import com.mosugar.recur.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryIntegrationTest extends PostgresContainerBase {

    @Autowired UserRepository userRepo;
    @Autowired SubscriptionRepository subRepo;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        subRepo.deleteAll();
        userRepo.deleteAll();

        user = userRepo.save(User.builder()
                .email("alice@example.com")
                .passwordHash("hash")
                .displayName("Alice")
                .mainCurrency(Currency.EUR)
                .build());

        otherUser = userRepo.save(User.builder()
                .email("bob@example.com")
                .passwordHash("hash")
                .displayName("Bob")
                .mainCurrency(Currency.USD)
                .build());
    }

    // ── UserRepository ────────────────────────────────────────────────────────

    @Test
    void findByEmail_returnsUser() {
        Optional<User> found = userRepo.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void findByEmail_notFound_returnsEmpty() {
        assertThat(userRepo.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_true() {
        assertThat(userRepo.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    void existsByEmail_false() {
        assertThat(userRepo.existsByEmail("nobody@example.com")).isFalse();
    }

    // ── SubscriptionRepository ────────────────────────────────────────────────

    private Subscription buildSub(User owner, String name, boolean active, Category cat) {
        return Subscription.builder()
                .user(owner)
                .name(name)
                .priceCents(999L)
                .currency(Currency.EUR)
                .cycle(BillingCycle.MONTHLY)
                .firstPaymentDate(LocalDate.of(2024, 1, 1))
                .category(cat)
                .active(active)
                .build();
    }

    @Test
    void findAllByUserId_returnsOnlyUserSubs() {
        subRepo.save(buildSub(user, "Netflix", true, Category.STREAMING));
        subRepo.save(buildSub(otherUser, "Spotify", true, Category.MUSIC));

        List<Subscription> results = subRepo.findAllByUserId(
                user.getId(), Sort.by("name"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Netflix");
    }

    @Test
    void findAllByUserIdAndActive_filtersInactive() {
        subRepo.save(buildSub(user, "Netflix", true, Category.STREAMING));
        subRepo.save(buildSub(user, "OldSub", false, Category.SOFTWARE));

        List<Subscription> active = subRepo.findAllByUserIdAndActive(
                user.getId(), true, Sort.by("name"));
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getName()).isEqualTo("Netflix");
    }

    @Test
    void findAllByUserIdAndCategory_filtersByCategory() {
        subRepo.save(buildSub(user, "Netflix", true, Category.STREAMING));
        subRepo.save(buildSub(user, "Spotify", true, Category.MUSIC));

        List<Subscription> streaming = subRepo.findAllByUserIdAndCategory(
                user.getId(), Category.STREAMING, Sort.by("name"));
        assertThat(streaming).hasSize(1);
        assertThat(streaming.get(0).getName()).isEqualTo("Netflix");
    }

    @Test
    void findByIdAndUserId_ownershipCheck() {
        Subscription sub = subRepo.save(buildSub(user, "Netflix", true, Category.STREAMING));

        // owner can find it
        assertThat(subRepo.findByIdAndUserId(sub.getId(), user.getId())).isPresent();
        // other user cannot
        assertThat(subRepo.findByIdAndUserId(sub.getId(), otherUser.getId())).isEmpty();
    }

    @Test
    void findAllByUserIdAndActiveTrue_returnsActiveOnly() {
        subRepo.save(buildSub(user, "Active", true, Category.OTHER));
        subRepo.save(buildSub(user, "Inactive", false, Category.OTHER));

        assertThat(subRepo.findAllByUserIdAndActiveTrue(user.getId())).hasSize(1);
    }

    @Test
    void findAllByUserIdAndIdIn_returnsOnlyMatchingIds() {
        Subscription s1 = subRepo.save(buildSub(user, "S1", true, Category.OTHER));
        Subscription s2 = subRepo.save(buildSub(user, "S2", true, Category.OTHER));
        subRepo.save(buildSub(otherUser, "S3", true, Category.OTHER));

        List<Subscription> result = subRepo.findAllByUserIdAndIdIn(
                user.getId(), List.of(s1.getId(), s2.getId()));
        assertThat(result).hasSize(2);
    }
}
