package com.mockerview.scheduler;

import com.mockerview.entity.Session;
import com.mockerview.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionScheduler.class);
    private final SessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SessionScheduler(SessionRepository sessionRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoStartScheduledSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Session> scheduledSessions = sessionRepository.findByStatusAndStartTimeBefore(
            Session.SessionStatus.PLANNED, now
        );
        
        if (!scheduledSessions.isEmpty()) {
            log.info("🚀 예약 세션 자동 시작 체크 - {}개 발견", scheduledSessions.size());
            
            for (Session session : scheduledSessions) {
                try {
                    session.setSessionStatus(Session.SessionStatus.RUNNING);
                    session.setStartTime(now);
                    sessionRepository.save(session);
                    
                    Map<String, Object> message = new HashMap<>();
                    message.put("sessionId", session.getId());
                    message.put("status", "RUNNING");
                    message.put("timestamp", now);
                    
                    messagingTemplate.convertAndSend(
                        "/topic/session/" + session.getId() + "/status", 
                        message
                    );
                    
                    log.info("✅ 세션 자동 시작 완료 - ID: {}, 제목: {}", 
                            session.getId(), session.getTitle());
                } catch (Exception e) {
                    log.error("❌ 세션 자동 시작 실패 - ID: {}", session.getId(), e);
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoExpireSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Session> expiredSessions = sessionRepository.findExpiredSessions(
            Session.SessionStatus.RUNNING, 
            now
        );
        
        int expiredCount = 0;
        for (Session session : expiredSessions) {
            session.setSessionStatus(Session.SessionStatus.ENDED);
            session.setEndTime(now);
            sessionRepository.save(session);
            
            sendExpirationNotification(session.getId(), "EXPIRED");
            
            expiredCount++;
        }
        
        if (expiredCount > 0) {
            log.info("⏰ 자동 만료된 세션 수: {}", expiredCount);
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoEndInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(3);
        
        List<Session> sessions = sessionRepository.findAll();
        int endedCount = 0;
        
        for (Session session : sessions) {
            if (session.getSessionStatus() == Session.SessionStatus.RUNNING && 
                session.getLastActivity() != null && 
                session.getLastActivity().isBefore(threshold)) {
                
                session.setSessionStatus(Session.SessionStatus.ENDED);
                session.setEndTime(LocalDateTime.now());
                sessionRepository.save(session);
                
                sendExpirationNotification(session.getId(), "INACTIVE");
                endedCount++;
            }
        }
        
        if (endedCount > 0) {
            log.info("💤 비활성으로 종료된 세션 수: {}", endedCount);
        }
    }
    
    private void sendExpirationNotification(Long sessionId, String reason) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("sessionId", sessionId);
            message.put("status", "ENDED");
            message.put("reason", reason);
            message.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/status", 
                message
            );
        } catch (Exception e) {
            log.error("만료 알림 전송 실패 - sessionId: {}", sessionId, e);
        }
    }
}