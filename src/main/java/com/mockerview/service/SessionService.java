package com.mockerview.service;

import com.mockerview.dto.AnswerMessage;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    @Transactional(readOnly = true)
    public Page<Session> getSelfInterviewsByHostIdPageable(Long hostId, Pageable pageable) {
        try {
            log.info("Getting paginated self-interview records for host {}. Page: {}, Size: {}",
                    hostId, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Session> sessionPage = sessionRepository.findByHostIdAndIsSelfInterviewPageable(
                hostId,
                "Y",
                pageable
            );
            
            log.info("Found {} total self-interviews across {} pages.", 
                    sessionPage.getTotalElements(), sessionPage.getTotalPages());
            
            return sessionPage;
        } catch (Exception e) {
            log.error("Error getting self-interview records: ", e);
            throw new RuntimeException("Failed to get self-interview records", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Session> getReviewableSessionsPageable(Pageable pageable) {
        try {
            log.info("Getting paginated reviewable sessions. Page: {}, Size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Session> sessionPage = sessionRepository.findByStatusAndIsReviewablePageable(
                Session.SessionStatus.ENDED,
                "Y",
                pageable
            );
            
            for (Session session : sessionPage.getContent()) {
                session.getQuestions().size();
                if (session.getHost() != null) {
                    session.getHost().getName();
                }
            }
            
            log.info("Found {} total reviewable sessions across {} pages.", 
                    sessionPage.getTotalElements(), sessionPage.getTotalPages());
            
            return sessionPage;
            
        } catch (Exception e) {
            log.error("Error getting reviewable sessions: ", e);
            throw new RuntimeException("Failed to get reviewable sessions", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Session> getSessionsPageable(Pageable pageable) {
        try {
            log.info("Getting paginated sessions with host information. Page: {}, Size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Session> sessionPage = sessionRepository.findAllSessionsWithHost(pageable);
            
            log.info("Found {} total sessions across {} pages.", 
                    sessionPage.getTotalElements(), sessionPage.getTotalPages());
            
            return sessionPage;
        } catch (Exception e) {
            log.error("Error getting paginated sessions: ", e);
            throw new RuntimeException("Failed to get paginated sessions", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Session> searchSessionsPageable(String keyword, Session.SessionStatus status, Pageable pageable) {
        try {
            log.info("Searching paginated sessions - keyword: {}, status: {}, Page: {}, Size: {}", 
                    keyword, status, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Session> sessionPage = sessionRepository.searchSessionsPageable(
                keyword != null && !keyword.isEmpty() ? keyword : null,
                status,
                pageable
            );
            
            log.info("Search result: {} total sessions across {} pages.", 
                    sessionPage.getTotalElements(), sessionPage.getTotalPages());
            
            return sessionPage;
        } catch (Exception e) {
            log.error("Error searching paginated sessions: ", e);
            throw new RuntimeException("Failed to search paginated sessions", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Session> getAllSessions() {
        try {
            log.info("Getting all sessions with host information...");
            List<Session> sessions = sessionRepository.findByOrderByCreatedAtDesc();
            log.info("Found {} sessions", sessions.size());
            return sessions;
        } catch (Exception e) {
            log.error("Error getting all sessions: ", e);
            throw new RuntimeException("Failed to get all sessions", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Session> searchSessions(String keyword, String status, String sortBy, String sortOrder) {
        try {
            log.info("Searching sessions - keyword: {}, status: {}, sortBy: {}, sortOrder: {}", 
                    keyword, status, sortBy, sortOrder);
            List<Session> sessions = sessionRepository.searchSessions(keyword, status, sortBy, sortOrder);
            log.info("Search result: {} sessions", sessions.size());
            return sessions;
        } catch (Exception e) {
            log.error("Error searching sessions: ", e);
            throw new RuntimeException("Failed to search sessions", e);
        }
    }

    @Transactional
    public void createSession(String title, Long hostId, String sessionType, LocalDateTime scheduledStartTime) {
        try {
            User host = userRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("Host not found"));
            
            String validSessionType = (sessionType != null && 
                (sessionType.equals("TEXT") || sessionType.equals("VOICE") || sessionType.equals("VIDEO")))
                ? sessionType : "TEXT";
            
            Session session = Session.builder()
                .title(title)
                .host(host)
                .sessionStatus(Session.SessionStatus.PLANNED)
                .sessionType(validSessionType)
                .startTime(scheduledStartTime)
                .mediaEnabled(validSessionType.equals("VIDEO") ? (short) 1 : (short) 0)
                .build();
            
            Session saved = sessionRepository.save(session);
            log.info("Session created with ID: {}, type: {}, scheduled: {}", 
                    saved.getId(), validSessionType, scheduledStartTime);
            
        } catch (Exception e) {
            log.error("Error creating session: ", e);
            throw new RuntimeException("Failed to create session", e);
        }
    }

    @Transactional
    public Long saveAnswerAndRequestFeedback(AnswerMessage message) {
        try {
            User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + message.getUserId()));
            
            Question question = questionRepository.findById(Long.parseLong(message.getQuestionId().toString()))
                .orElseThrow(() -> new RuntimeException("Question not found: " + message.getQuestionId()));
            
            Answer answer = Answer.builder()
                .question(question)
                .user(user)
                .answerText(message.getAnswerText())
                .score(message.getScore())
                .build();
            
            Answer saved = answerRepository.save(answer);
            log.info("Answer saved with ID: {}", saved.getId());
            return saved.getId();
            
        } catch (Exception e) {
            log.error("Error saving answer: ", e);
            throw new RuntimeException("Failed to save answer", e);
        }
    }

    @Transactional(readOnly = true)
    public Session findById(Long sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    @Transactional
    public void startSession(Long sessionId) {
        Session session = findById(sessionId);
        session.setSessionStatus(Session.SessionStatus.RUNNING);
        session.setStartTime(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("Session {} started", sessionId);
    }

    @Transactional
    public void endSession(Long sessionId) {
        Session session = findById(sessionId);
        session.setSessionStatus(Session.SessionStatus.ENDED);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("Session {} ended", sessionId);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByHostId(Long hostId) {
        return sessionRepository.findByHostId(hostId);
    }

    @Transactional(readOnly = true)
    public List<Session> getReviewableSessions() {
        return sessionRepository.findByStatusAndIsReviewable(
            Session.SessionStatus.ENDED,
            "Y"
        );
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByHostIdAndType(Long hostId, String sessionType) {
        return sessionRepository.findByHostIdAndSessionType(hostId, sessionType);
    }

    @Transactional(readOnly = true)
    public boolean isHost(Long sessionId, Long userId) {
        return sessionRepository.isHost(sessionId, userId);
    }

    @Transactional(readOnly = true)
    public Long countByStatus(Session.SessionStatus status) {
        return sessionRepository.countBySessionStatus(status);
    }

    @Transactional
    public void updateLastActivity(Long sessionId) {
        Session session = findById(sessionId);
        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<Session> getSelfInterviewsByHostId(Long hostId) {
        return sessionRepository.findSelfInterviewsByHostId(hostId);
    }

    @Transactional(readOnly = true)
    public Long countNonSelfInterviewSessions() {
        return sessionRepository.countNonSelfInterviewSessions();
    }

    @Transactional(readOnly = true)
    public Long countByStatusAndIsSelfInterview(Session.SessionStatus status, String isSelfInterview) {
        return sessionRepository.countBySessionStatusAndIsSelfInterview(status, isSelfInterview);
    }
}