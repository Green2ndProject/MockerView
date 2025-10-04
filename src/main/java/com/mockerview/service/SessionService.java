package com.mockerview.service;

import com.mockerview.dto.AnswerMessage;
import com.mockerview.dto.SessionStatusMessage;
import com.mockerview.entity.Answer;
import com.mockerview.entity.Question;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService {
    
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    
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
            throw new RuntimeException("페이지별 세션 목록 조회 실패", e);
            }
    }

    public Page<Session> searchSessionsPageable(String keyword, String status, Pageable pageable) {
    try {
        log.info("Searching paginated sessions - keyword: {}, status: {}, Page: {}, Size: {}", 
            keyword, status, pageable.getPageNumber(), pageable.getPageSize());
        
        Session.SessionStatus sessionStatus = null;
        if (status != null && !status.isEmpty()) {
            sessionStatus = Session.SessionStatus.valueOf(status);
        }

        Page<Session> sessionPage = sessionRepository.searchSessionsPageable(keyword, sessionStatus, pageable);
        
        log.info("Search result: {} total sessions across {} pages.", 
            sessionPage.getTotalElements(), sessionPage.getTotalPages());
            
        return sessionPage;
        } catch (Exception e) {
            log.error("Error searching paginated sessions: ", e);
            throw new RuntimeException("페이지별 세션 검색 실패", e);
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
            throw new RuntimeException("세션 목록 조회 실패", e);
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
            throw new RuntimeException("세션 검색 실패", e);
        }
    }

    public Session createSession(String title, Long hostId, String sessionType) {
        try {
            User host = userRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("Host not found: " + hostId));
            
            String validSessionType = sessionType;
            if (!"TEXT".equals(sessionType) && !"AUDIO".equals(sessionType) && !"VIDEO".equals(sessionType)) {
                validSessionType = "TEXT";
            }
            
            Session session = Session.builder()
                .title(title)
                .host(host)
                .status(Session.SessionStatus.PLANNED)
                .sessionType(validSessionType)
                .mediaEnabled(!"TEXT".equals(validSessionType))
                .isReviewable("Y")
                .createdAt(LocalDateTime.now())
                .build();
            
            Session saved = sessionRepository.save(session);
            log.info("Session created with ID: {}, type: {}", saved.getId(), validSessionType);
            return saved;
        } catch (Exception e) {
            log.error("Error creating session: ", e);
            throw new RuntimeException("세션 생성 실패", e);
        }
    }

    public Long saveAnswer(AnswerMessage message) {
        try {
            User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + message.getUserId()));
            
            Question question = questionRepository.findById(Long.parseLong(message.getQuestionId().toString()))
                .orElseThrow(() -> new RuntimeException("Question not found: " + message.getQuestionId()));
            
            Answer answer = Answer.builder()
                .user(user)
                .question(question)
                .answerText(message.getAnswerText())
                .score(message.getScore())
                .build();
            
            Answer saved = answerRepository.save(answer);
            log.info("Answer saved with ID: {}", saved.getId());
            
            return saved.getId();
            
        } catch (Exception e) {
            log.error("Error saving answer: ", e);
            throw new RuntimeException("답변 저장 실패", e);
        }
    }

    public Long saveQuestion(Long sessionId, String questionText, Integer orderNo, Long questionerId,  Integer timer ) {
        try {
            Session session = findById(sessionId);
            
            if (session.getStatus() == Session.SessionStatus.PLANNED) {
                log.info("세션 상태 변경: PLANNED -> RUNNING (Session ID: {})", sessionId);
                session.setStatus(Session.SessionStatus.RUNNING);
                session.setStartTime(LocalDateTime.now());
                sessionRepository.save(session);
            }

            User questioner = userRepository.findById(questionerId)
                .orElseThrow(() -> new RuntimeException("Questioner not found: " + questionerId));
            
            Question question = Question.builder()
                .session(session)
                .text(questionText)
                .orderNo(orderNo != null ? orderNo : 1)
                .questioner(questioner)
                .timer(timer)
                .build();
            
            Question saved = questionRepository.save(question);
            log.info("Question saved with ID: {}", saved.getId());
            
            return saved.getId();
            
        } catch (Exception e) {
            log.error("Error saving question: ", e);
            throw new RuntimeException("질문 저장 실패", e);
        }
    }

    public Long saveQuestion(Long sessionId, String questionText, Integer orderNo, Integer timer) {
        return saveQuestion(sessionId, questionText, orderNo, 1L, timer); 
    }

    @Transactional(readOnly = true)
    public SessionStatusMessage getSessionStatus(Long sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
            
            List<String> participants = answerRepository.findDistinctUserNamesBySessionId(sessionId);
            
            Long questionCount = questionRepository.countBySessionId(sessionId);
            Long answerCount = answerRepository.countBySessionId(sessionId);
            
            return SessionStatusMessage.builder()
                .sessionId(sessionId)
                .status(session.getStatus().toString())
                .participants(participants)
                .questionCount(questionCount.intValue())
                .answerCount(answerCount.intValue())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting session status: ", e);
            throw new RuntimeException("세션 상태 조회 실패", e);
        }
    }

    @Transactional(readOnly = true)
    public Session findById(Long sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    @Transactional(readOnly = true)
    public List<Session> findAllSessions() {
        return sessionRepository.findAll();
    }

    public Session startSession(Long sessionId) {
        Session session = findById(sessionId);
        session.setStatus(Session.SessionStatus.RUNNING);
        session.setStartTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public Session endSession(Long sessionId) {
        Session session = findById(sessionId);
        
        if (session.getStatus() != Session.SessionStatus.ENDED) {
            log.info("세션 상태 변경: RUNNING -> ENDED (Session ID: {})", sessionId);
            session.setStatus(Session.SessionStatus.ENDED);
            session.setEndTime(LocalDateTime.now());
            return sessionRepository.save(session);
        }
        
        return session;
    }

    @Transactional(readOnly = true)
    public List<Question> getSessionQuestions(Long sessionId) {
        return questionRepository.findBySessionIdOrderByOrderNo(sessionId);
    }

    @Transactional(readOnly = true)
    public List<Answer> getSessionAnswers(Long sessionId) {
        return answerRepository.findByQuestionSessionIdOrderByCreatedAt(sessionId);
    }
}