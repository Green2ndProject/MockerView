package com.mockerview.repository;

import com.mockerview.entity.Subscription;
import com.mockerview.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserAndStatus(User user, Subscription.SubscriptionStatus status);
    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);
}
