package com.mockerview.service;

import com.mockerview.entity.Session;
import com.mockerview.entity.Subscription;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionMigrationService {

    private final SessionRepository sessionRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void migrateExistingSelfInterviews() {
        log.info("🔄 기존 셀프면접 세션 마이그레이션 시작");

        List<Session> allSelfInterviews = sessionRepository.findByIsSelfInterviewOrderByCreatedAtDesc("Y");
        
        if (allSelfInterviews.isEmpty()) {
            log.info("마이그레이션할 셀프면접 세션이 없습니다.");
            return;
        }

        Map<Long, Integer> userSessionCounts = new HashMap<>();

        for (Session session : allSelfInterviews) {
            if (session.getHost() != null) {
                Long userId = session.getHost().getId();
                userSessionCounts.put(userId, userSessionCounts.getOrDefault(userId, 0) + 1);
            }
        }

        int totalUpdated = 0;
        for (Map.Entry<Long, Integer> entry : userSessionCounts.entrySet()) {
            Long userId = entry.getKey();
            Integer sessionCount = entry.getValue();

            Subscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.ACTIVE)
                .orElse(null);

            if (subscription != null) {
                int currentUsed = subscription.getUsedSessions();
                int newUsed = currentUsed + sessionCount;
                
                subscription.setUsedSessions(Math.min(newUsed, subscription.getSessionLimit()));
                subscriptionRepository.save(subscription);

                log.info("✅ userId: {} - 기존: {}, 추가: {}, 새로운 총합: {}/{}", 
                    userId, currentUsed, sessionCount, subscription.getUsedSessions(), subscription.getSessionLimit());
                
                totalUpdated++;
            } else {
                log.warn("⚠️ userId: {}에 활성 구독이 없음 (셀프면접 {}개)", userId, sessionCount);
            }
        }

        log.info("🎉 마이그레이션 완료 - 총 {}명 사용자의 세션 카운트 업데이트, 총 {}개 셀프면접 반영", 
            totalUpdated, allSelfInterviews.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMigrationPreview() {
        List<Session> allSelfInterviews = sessionRepository.findByIsSelfInterviewOrderByCreatedAtDesc("Y");
        
        Map<Long, Integer> userSessionCounts = new HashMap<>();
        for (Session session : allSelfInterviews) {
            if (session.getHost() != null) {
                Long userId = session.getHost().getId();
                userSessionCounts.put(userId, userSessionCounts.getOrDefault(userId, 0) + 1);
            }
        }

        Map<String, Object> preview = new HashMap<>();
        preview.put("totalSelfInterviews", allSelfInterviews.size());
        preview.put("affectedUsers", userSessionCounts.size());
        preview.put("userDetails", userSessionCounts);

        return preview;
    }
}
