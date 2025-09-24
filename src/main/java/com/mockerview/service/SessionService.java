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

    public Long saveAnswer(AnswerMessage message) {
        try {
            User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + message.getUserId()));
            
            Question question = questionRepository.findById(Long.parseLong(message.getQuestionId().toString()))
                .orElseThrow(() -> new RuntimeException("Question not found: " + message.getQuestionId()));
            
            Answer answer = Answer.builder()
                .user(user)
                .question(question)
                .text(message.getAnswerText())
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

    public Long saveQuestion(Long sessionId, String questionText, Integer orderNo) {
        try {
            Session session = findById(sessionId);
            
            Question question = Question.builder()
                .session(session)
                .text(questionText)
                .orderNo(orderNo != null ? orderNo : 1)
                .build();
            
            Question saved = questionRepository.save(question);
            log.info("Question saved with ID: {}", saved.getId());
            
            return saved.getId();
            
        } catch (Exception e) {
            log.error("Error saving question: ", e);
            throw new RuntimeException("질문 저장 실패", e);
        }
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
        session.setStatus(Session.SessionStatus.ENDED);
        session.setEndTime(LocalDateTime.now());
        return sessionRepository.save(session);
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