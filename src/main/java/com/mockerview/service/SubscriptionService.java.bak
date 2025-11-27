package com.mockerview.service;

import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();
    
    private static final Map<Subscription.PlanType, Integer> PLAN_LIMITS = Map.of(
        Subscription.PlanType.FREE, 5,
        Subscription.PlanType.BASIC, 30,
        Subscription.PlanType.PRO, 100,
        Subscription.PlanType.TEAM, 500,
        Subscription.PlanType.ENTERPRISE, Integer.MAX_VALUE
    );
    
    private static final Map<Subscription.PlanType, Integer> REVIEW_READ_LIMITS = Map.of(
        Subscription.PlanType.FREE, 3,
        Subscription.PlanType.BASIC, 50,
        Subscription.PlanType.PRO, Integer.MAX_VALUE,
        Subscription.PlanType.TEAM, Integer.MAX_VALUE,
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
            .reviewReadLimit(REVIEW_READ_LIMITS.get(Subscription.PlanType.FREE))
            .usedReviewReads(0)
            .autoRenew(false)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusMonths(1))
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .build();
        subscriptionRepository.save(sub);
        log.info("FREE 구독 생성 완료 - userId: {}", user.getId());
    }
    
    @Transactional
    public Subscription createSubscription(Long userId, Subscription.PlanType planType) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription current = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
        
        if (current != null) {
            current.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(current);
        }
        
        Subscription newSub = Subscription.builder()
            .user(user)
            .planType(planType)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .sessionLimit(PLAN_LIMITS.get(planType))
            .usedSessions(0)
            .autoRenew(true)
            .startDate(LocalDateTime.now())
            .endDate(LocalDateTime.now().plusMonths(1))
            .nextBillingDate(LocalDateTime.now().plusMonths(1))
            .build();
        
        subscriptionRepository.save(newSub);
        log.info("구독 생성 완료 - userId: {}, plan: {}", userId, planType);
        
        return newSub;
    }
    
    @Transactional
    public boolean canCreateSessionAndIncrement(Long userId) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        
        synchronized (lock) {
            Subscription sub = subscriptionRepository
                .findByUserIdAndStatusWithLock(userId, Subscription.SubscriptionStatus.ACTIVE)
                .orElse(null);
            
            if (sub == null) {
                log.warn("활성 구독 없음 - userId: {}", userId);
                return false;
            }
            
            if (sub.getUsedSessions() >= sub.getSessionLimit()) {
                log.warn("세션 한도 초과 - userId: {}, used: {}/{}", 
                    userId, sub.getUsedSessions(), sub.getSessionLimit());
                return false;
            }
            
            sub.setUsedSessions(sub.getUsedSessions() + 1);
            subscriptionRepository.save(sub);
            subscriptionRepository.flush();
            
            log.info("✅ 세션 예약 성공 - userId: {}, used: {}/{}", 
                userId, sub.getUsedSessions(), sub.getSessionLimit());
            
            return true;
        }
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
        
        if (sub.getUsedSessions() >= sub.getSessionLimit()) {
            log.warn("❌ 세션 한도 초과 - userId: {}, used: {}/{}", 
                userId, sub.getUsedSessions(), sub.getSessionLimit());
            throw new RuntimeException("SESSION_LIMIT_EXCEEDED");
        }
        
        sub.setUsedSessions(sub.getUsedSessions() + 1);
        subscriptionRepository.save(sub);
        
        log.info("세션 사용 증가 - userId: {}, used: {}/{}", 
            userId, sub.getUsedSessions(), sub.getSessionLimit());
    }
    
    @Transactional
    public void decrementUsedSessions(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new RuntimeException("Active subscription not found"));
        
        if (sub.getUsedSessions() > 0) {
            sub.setUsedSessions(sub.getUsedSessions() - 1);
            subscriptionRepository.save(sub);
            
            log.info("세션 사용 감소 (롤백) - userId: {}, used: {}/{}", 
                userId, sub.getUsedSessions(), sub.getSessionLimit());
        }
    }
    
    @Transactional
    public void upgradePlan(User user, Subscription.PlanType newPlan) {
        Subscription current = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
        
        LocalDateTime newStartDate = LocalDateTime.now();
        LocalDateTime newEndDate = LocalDateTime.now().plusMonths(1);
        LocalDateTime newNextBilling = LocalDateTime.now().plusMonths(1);
        
        if (current != null) {
            current.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(current);
            log.info("기존 구독 취소 - subscriptionId: {}, plan: {}", current.getId(), current.getPlanType());
        }
        
        Subscription newSub = Subscription.builder()
            .user(user)
            .planType(newPlan)
            .status(Subscription.SubscriptionStatus.ACTIVE)
            .sessionLimit(PLAN_LIMITS.get(newPlan))
            .usedSessions(0)
            .autoRenew(true)
            .startDate(newStartDate)
            .endDate(newEndDate)
            .nextBillingDate(newNextBilling)
            .build();
        
        subscriptionRepository.save(newSub);
        log.info("플랜 업그레이드 완료 - userId: {}, oldPlan: {}, newPlan: {}, startDate: {}, endDate: {}", 
            user.getId(), 
            current != null ? current.getPlanType() : "NONE", 
            newPlan,
            newStartDate,
            newEndDate);
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
    
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionLimitInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
        
        if (sub == null) {
            return Map.of(
                "canCreate", false,
                "limitReached", true,
                "usedSessions", 0,
                "sessionLimit", 0,
                "currentPlan", "NONE",
                "message", "활성 구독이 없습니다."
            );
        }
        
        boolean limitReached = sub.getUsedSessions() >= sub.getSessionLimit();
        
        return Map.of(
            "canCreate", !limitReached,
            "limitReached", limitReached,
            "usedSessions", sub.getUsedSessions(),
            "sessionLimit", sub.getSessionLimit(),
            "currentPlan", sub.getPlanType().name(),
            "message", limitReached ? "세션 생성 한도에 도달했습니다." : "세션 생성 가능합니다."
        );
    }
    
    @Transactional
    public Session createSelfInterviewAtomic(Long userId) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        
        synchronized (lock) {
            log.info("🔒 원자적 셀프면접 생성 시작 - userId: {}", userId);
            
            Subscription sub = subscriptionRepository
                .findByUserIdAndStatusWithLock(userId, Subscription.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("활성 구독이 없습니다"));
            
            if (sub.getUsedSessions() >= sub.getSessionLimit()) {
                log.warn("❌ 세션 한도 초과 - userId: {}, used: {}/{}", 
                    userId, sub.getUsedSessions(), sub.getSessionLimit());
                throw new RuntimeException("SESSION_LIMIT_EXCEEDED");
            }
            
            sub.setUsedSessions(sub.getUsedSessions() + 1);
            subscriptionRepository.saveAndFlush(sub);
            
            log.info("✅ 카운트 증가 완료 - userId: {}, used: {}/{}", 
                userId, sub.getUsedSessions(), sub.getSessionLimit());
            
            User host = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Session session = Session.builder()
                .title(host.getName() + "님의 셀프 면접")
                .host(host)
                .sessionType("TEXT")
                .sessionStatus(Session.SessionStatus.RUNNING)
                .isSelfInterview("Y")
                .aiEnabled(true)
                .aiMode("REALTIME")
                .allowParticipantsToggleAi(false)
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
            
            session = sessionRepository.saveAndFlush(session);
            
            log.info("✅ 셀프면접 생성 완료 - sessionId: {}, userId: {}, used: {}/{}", 
                session.getId(), userId, sub.getUsedSessions(), sub.getSessionLimit());
            
            return session;
        }
    }
    
    @Transactional
    public Session createTeamSessionAtomic(Long userId, String title, String sessionType, short mediaEnabled) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        
        synchronized (lock) {
            log.info("🔒 원자적 팀 세션 생성 시작 - userId: {}", userId);
            
            Subscription sub = subscriptionRepository
                .findByUserIdAndStatusWithLock(userId, Subscription.SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("활성 구독이 없습니다"));
            
            if (sub.getUsedSessions() >= sub.getSessionLimit()) {
                log.warn("❌ 세션 한도 초과 - userId: {}, used: {}/{}", 
                    userId, sub.getUsedSessions(), sub.getSessionLimit());
                throw new RuntimeException("SESSION_LIMIT_EXCEEDED");
            }
            
            sub.setUsedSessions(sub.getUsedSessions() + 1);
            subscriptionRepository.saveAndFlush(sub);
            
            log.info("✅ 카운트 증가 완료 - userId: {}, used: {}/{}", 
                userId, sub.getUsedSessions(), sub.getSessionLimit());
            
            User host = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Session session = Session.builder()
                .title(title)
                .host(host)
                .sessionType(sessionType)
                .mediaEnabled(mediaEnabled)
                .sessionStatus(Session.SessionStatus.PLANNED)
                .isSelfInterview("N")
                .createdAt(LocalDateTime.now())
                .build();
            
            session = sessionRepository.saveAndFlush(session);
            
            log.info("✅ 팀 세션 생성 완료 - sessionId: {}, userId: {}, used: {}/{}", 
                session.getId(), userId, sub.getUsedSessions(), sub.getSessionLimit());
            
            return session;
        }
    }
    
    @Transactional
    public boolean canReadReviewAndIncrement(Long userId) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
        
        synchronized (lock) {
            Subscription sub = subscriptionRepository
                .findByUserAndStatus(userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found")), 
                    Subscription.SubscriptionStatus.ACTIVE)
                .orElse(null);
            
            if (sub == null) {
                log.warn("활성 구독 없음 - userId: {}", userId);
                return false;
            }
            
            if (sub.getReviewReadLimit() == null) {
                Integer defaultLimit = REVIEW_READ_LIMITS.get(sub.getPlanType());
                sub.setReviewReadLimit(defaultLimit);
                log.info("리뷰 읽기 한도 초기화 - userId: {}, limit: {}", userId, defaultLimit);
            }
            
            if (sub.getUsedReviewReads() == null) {
                sub.setUsedReviewReads(0);
            }
            
            if (sub.getUsedReviewReads() >= sub.getReviewReadLimit()) {
                log.warn("리뷰 읽기 한도 초과 - userId: {}, used: {}/{}", 
                    userId, sub.getUsedReviewReads(), sub.getReviewReadLimit());
                return false;
            }
            
            sub.setUsedReviewReads(sub.getUsedReviewReads() + 1);
            subscriptionRepository.save(sub);
            subscriptionRepository.flush();
            
            log.info("리뷰 읽기 카운트 증가 - userId: {}, used: {}/{}", 
                userId, sub.getUsedReviewReads(), sub.getReviewReadLimit());
            
            return true;
        }
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getReviewReadInfo(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new RuntimeException("활성 구독이 없습니다"));
        
        Integer usedReads = sub.getUsedReviewReads() != null ? sub.getUsedReviewReads() : 0;
        Integer readLimit = sub.getReviewReadLimit() != null ? sub.getReviewReadLimit() : REVIEW_READ_LIMITS.get(sub.getPlanType());
        
        boolean canRead = usedReads < readLimit;
        
        return Map.of(
            "canRead", canRead,
            "limitReached", !canRead,
            "usedReads", usedReads,
            "readLimit", readLimit,
            "currentPlan", sub.getPlanType().name(),
            "message", canRead ? "리뷰를 읽을 수 있습니다" : "리뷰 읽기 한도에 도달했습니다"
        );
    }
    
    @Transactional(readOnly = true)
    public boolean canReadReview(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Subscription sub = subscriptionRepository
            .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE)
            .orElse(null);
        
        if (sub == null) {
            log.warn("활성 구독 없음 - userId: {}", userId);
            return false;
        }
        
        Integer usedReads = sub.getUsedReviewReads() != null ? sub.getUsedReviewReads() : 0;
        Integer readLimit = sub.getReviewReadLimit() != null ? sub.getReviewReadLimit() : REVIEW_READ_LIMITS.get(sub.getPlanType());
        
        return usedReads < readLimit;
    }
}