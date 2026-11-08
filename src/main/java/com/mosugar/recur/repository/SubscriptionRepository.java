package com.mosugar.recur.repository;

import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.enums.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // List all subscriptions for a user (optional active + category filters applied in service)
    List<Subscription> findAllByUserId(UUID userId, Sort sort);

    List<Subscription> findAllByUserIdAndActive(UUID userId, boolean active, Sort sort);

    List<Subscription> findAllByUserIdAndCategory(UUID userId, Category category, Sort sort);

    List<Subscription> findAllByUserIdAndActiveAndCategory(
            UUID userId, boolean active, Category category, Sort sort);

    // Ownership check — returns empty if the subscription doesn't belong to the user
    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);

    // All active subscriptions for a user (for stats / upcoming)
    List<Subscription> findAllByUserIdAndActiveTrue(UUID userId);

    // Bulk load by IDs for the sync endpoint (only the user's own records)
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.id IN :ids")
    List<Subscription> findAllByUserIdAndIdIn(@Param("userId") UUID userId,
                                              @Param("ids") List<UUID> ids);
}
