package com.mockerview.scheduler;

import com.mockerview.entity.Subscription;
import com.mockerview.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {
    
    private final SubscriptionRepository subscriptionRepository;
    
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expireSubscriptions() {
        log.info("구독 만료 체크 시작");
        
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByStatus(Subscription.SubscriptionStatus.ACTIVE);
        
        int expiredCount = 0;
        
        for (Subscription subscription : activeSubscriptions) {
            if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(now)) {
                subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(subscription);
                expiredCount++;
                
                log.info("구독 만료 처리 - subscriptionId: {}, userId: {}", 
                    subscription.getId(), subscription.getUser().getId());
            }
        }
        
        log.info("구독 만료 체크 완료 - 만료 처리: {}건", expiredCount);
    }
    
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void resetMonthlySessions() {
        log.info("월간 세션 사용량 초기화 시작");
        
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByStatus(Subscription.SubscriptionStatus.ACTIVE);
        
        int resetCount = 0;
        
        for (Subscription subscription : activeSubscriptions) {
            if (subscription.getNextBillingDate() != null && 
                subscription.getNextBillingDate().isBefore(now)) {
                
                subscription.setUsedSessions(0);
                subscription.setNextBillingDate(subscription.getNextBillingDate().plusMonths(1));
                subscriptionRepository.save(subscription);
                resetCount++;
                
                log.info("월간 세션 초기화 - subscriptionId: {}, userId: {}", 
                    subscription.getId(), subscription.getUser().getId());
            }
        }
        
        log.info("월간 세션 초기화 완료 - 초기화: {}건", resetCount);
    }
    
    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void sendExpiryNotifications() {
        log.info("만료 알림 전송 시작");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);
        
        List<Subscription> activeSubscriptions = subscriptionRepository
            .findByStatus(Subscription.SubscriptionStatus.ACTIVE);
        
        int notificationCount = 0;
        
        for (Subscription subscription : activeSubscriptions) {
            if (subscription.getEndDate() != null && 
                subscription.getEndDate().isAfter(now) && 
                subscription.getEndDate().isBefore(threeDaysLater)) {
                
                log.info("만료 예정 알림 대상 - subscriptionId: {}, userId: {}, endDate: {}", 
                    subscription.getId(), 
                    subscription.getUser().getId(), 
                    subscription.getEndDate());
                
                notificationCount++;
            }
        }
        
        log.info("만료 알림 전송 완료 - 알림 대상: {}건", notificationCount);
    }
}
