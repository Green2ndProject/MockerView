package com.mockerview.repository;

import com.mockerview.entity.BadgeType;
import com.mockerview.entity.User;
import com.mockerview.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserId(Long userId);
    List<UserBadge> findByUserOrderByEarnedAtDesc(User user);
    boolean existsByUserAndBadgeType(User user, BadgeType badgeType);
    long countByUser(User user);
}
