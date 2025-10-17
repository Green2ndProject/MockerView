package com.mockerview.service;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    
    private static final Map<Subscription.PlanType, Integer> PLAN_LIMITS = Map.of(
        Subscription.PlanType.FREE, 5,
        Subscription.PlanType.BASIC, 30,
        Subscription.PlanType.PRO, 100,
        Subscription.PlanType.TEAM, 500,
        Subscription.PlanType.ENTERPRISE, Integer.MAX_VALUE
    );
    
    private static final Map<Subscription.PlanType, Integer> PLAN_PRICES = Map.of(
        Subscription.PlanType.FREE, 0,
        Subscription.PlanType.BASIC, 9900,
        Subscription.PlanType.PRO, 29900,
        Subscription.PlanType.TEAM, 99000,
        Subscription.PlanType.ENTERPRISE, 0
    );
    
    @Transactional
    public void createFreeSubscription(User user) {
        Subscription sub = Subscription.builder()
            .user(user)
            .planType(Subscription.PlanType.FREE)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .sessionLimit(PLAN_LIMITS.get(Subscription.PlanType.FREE))
            .usedSessions(0)
            .autoRenew(false)
            .startDate(LocalDateTime.now())
            .build();
        subscriptionRepository.save(sub);
        log.info("FREE 구독 생성 완료 - userId: {}", user.getId());
    }
    
    @Transactional(readOnly = true)
    public boolean canCreateSession(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
        
        if (sub == null) {
            log.warn("활성 구독 없음 - userId: {}", userId);
            return false;
        }
        
        boolean canCreate = sub.getUsedSessions() < sub.getSessionLimit();
        log.info("세션 생성 가능 여부 - userId: {}, used: {}/{}, canCreate: {}", 
            userId, sub.getUsedSessions(), sub.getSessionLimit(), canCreate);
        
        return canCreate;
    }
    
    @Transactional
    public void incrementUsedSessions(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new RuntimeException("Active subscription not found"));
        
        sub.setUsedSessions(sub.getUsedSessions() + 1);
        subscriptionRepository.save(sub);
        
        log.info("세션 사용 증가 - userId: {}, used: {}/{}", 
            userId, sub.getUsedSessions(), sub.getSessionLimit());
    }
    
    @Transactional
    public void upgradePlan(User user, Subscription.PlanType newPlan) {
        Subscription current = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
        
        if (current != null) {
            current.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(current);
        }
        
        Subscription newSub = Subscription.builder()
            .user(user)
            .planType(newPlan)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .sessionLimit(PLAN_LIMITS.get(newPlan))
            .usedSessions(0)
            .autoRenew(true)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusMonths(1))
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .build();
        
        subscriptionRepository.save(newSub);
        log.info("플랜 업그레이드 완료 - userId: {}, plan: {}", user.getId(), newPlan);
    }
    
    @Transactional(readOnly = true)
    public Subscription getActiveSubscription(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
    }
    
    public Integer getPlanPrice(Subscription.PlanType planType) {
        return PLAN_PRICES.get(planType);
    }
}
