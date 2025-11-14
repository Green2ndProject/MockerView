package com.mockerview.repository;

import com.mockerview.entity.Subscription;
import com.mockerview.entity.Subscription.SubscriptionStatus;
import com.mockerview.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserAndStatus(User user, Subscription.SubscriptionStatus status);

    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = :status ORDER BY s.createdAt DESC")
    List<Subscription> findByUserIdAndStatusWithLockList(@Param("userId") Long userId,
            @Param("status") SubscriptionStatus status, Pageable pageable);
    
    default Optional<Subscription> findByUserIdAndStatusWithLock(Long userId, SubscriptionStatus status) {
            List<Subscription> subscriptions = findByUserIdAndStatusWithLockList(userId, status, Pageable.ofSize(1));
            return subscriptions.isEmpty() ? Optional.empty() : Optional.of(subscriptions.get(0));
    }
    
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = :status ORDER BY s.createdAt DESC")
    List<Subscription> findByUserIdAndStatusList(@Param("userId") Long userId,
            @Param("status") SubscriptionStatus status, Pageable pageable);
    
    default Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status) {
            List<Subscription> subscriptions = findByUserIdAndStatusList(userId, status, Pageable.ofSize(1));
            return subscriptions.isEmpty() ? Optional.empty() : Optional.of(subscriptions.get(0));
    }

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") Long userId);
}
