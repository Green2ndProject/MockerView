package com.mockerview.controller.websocket;

import com.mockerview.entity.Session;
import com.mockerview.repository.SessionRepository;
import com.mockerview.service.BadgeService;
import com.mockerview.service.InterviewReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEndHandler {

    private final SessionRepository sessionRepository;
    private final InterviewReportService reportService;
    private final BadgeService badgeService;

    @Async
    @Transactional
    public void handleSessionEnd(Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));

            if (session.getStatus() != Session.SessionStatus.ENDED) {
                session.setStatus(Session.SessionStatus.ENDED);
                sessionRepository.save(session);
            }

            log.info("🎬 세션 종료 처리 시작: Session {}", sessionId);

            reportService.generateReportAsync(sessionId);
            log.info("✅ 리포트 생성 요청 완료: Session {}", sessionId);

            badgeService.checkAndAwardBadges(session.getHost(), session);
            log.info("✅ 배지 체크 완료: Session {}", sessionId);

        } catch (Exception e) {
            log.error("❌ 세션 종료 처리 실패: {}", e.getMessage(), e);
        }
    }
}
