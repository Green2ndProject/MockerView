package com.mockerview.scheduler;

import com.mockerview.entity.Session;
import com.mockerview.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionScheduler.class);
    private final SessionRepository sessionRepository;

    public SessionScheduler(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
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
                endedCount++;
            }
        }
        
        if (endedCount > 0) {
            log.info("자동 종료된 세션 수: {}", endedCount);
        }
    }
    
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void autoStartScheduledSessions() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Session> scheduledSessions = sessionRepository.findByStatusAndStartTimeBefore(
            Session.SessionStatus.PLANNED, now
        );
        
        for (Session session : scheduledSessions) {
            session.setSessionStatus(Session.SessionStatus.RUNNING);
            log.info("예약 세션 자동 시작 - ID: {}, 제목: {}, 예약시각: {}", 
                    session.getId(), session.getTitle(), session.getStartTime());
        }
        
        if (!scheduledSessions.isEmpty()) {
            sessionRepository.saveAll(scheduledSessions);
            log.info("총 {} 개 예약 세션 자동 시작됨", scheduledSessions.size());
        }
    }
}
