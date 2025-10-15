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
        log.info("🎬 SessionScheduler 초기화 완료!");
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoStartScheduledSessions() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("⏰ 스케줄러 실행 중 - 현재 시각: {}", now);
            
            List<Session> scheduledSessions = sessionRepository.findByStatusAndStartTimeBefore(
                Session.SessionStatus.PLANNED, now
            );
            
            log.info("🔍 자동 시작 대상 세션: {}개", scheduledSessions.size());
            
            if (scheduledSessions.isEmpty()) {
                log.info("✅ 자동 시작할 세션 없음");
                return;
            }
            
            for (Session session : scheduledSessions) {
                try {
                    log.info("🚀 세션 자동 시작 - ID: {}, 제목: {}, 예정: {}", 
                            session.getId(), session.getTitle(), session.getStartTime());
                    
                    session.setStatus(Session.SessionStatus.RUNNING);
                    session.setStartTime(now);
                    Session saved = sessionRepository.save(session);
                    
                    log.info("💾 DB 저장 완료 - ID: {}, 새 상태: {}", saved.getId(), saved.getStatus());
                    
                    Map<String, Object> message = new HashMap<>();
                    message.put("sessionId", session.getId());
                    message.put("status", "RUNNING");
                    message.put("timestamp", now);
                    
                    messagingTemplate.convertAndSend(
                        "/topic/session/" + session.getId() + "/status", 
                        message
                    );
                    
                    log.info("✅ 세션 자동 시작 완료 - ID: {}", session.getId());
                } catch (Exception e) {
                    log.error("❌ 세션 자동 시작 실패 - ID: {}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("💥 스케줄러 실행 오류", e);
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
            session.setStatus(Session.SessionStatus.ENDED);
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
            if (session.getStatus() == Session.SessionStatus.RUNNING && 
                session.getLastActivity() != null && 
                session.getLastActivity().isBefore(threshold)) {
                
                session.setStatus(Session.SessionStatus.ENDED);
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