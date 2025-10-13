package com.mockerview.controller.websocket;

import com.mockerview.dto.SessionJoinMessage;
import com.mockerview.dto.SessionStatusMessage;
import com.mockerview.dto.websocket.AnswerMessage;
import com.mockerview.dto.websocket.InterviewerFeedbackMessage;
import com.mockerview.dto.websocket.QuestionMessage;
import com.mockerview.entity.*;
import com.mockerview.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    private final Map<String, Set<String>> sessionParticipants = new ConcurrentHashMap<>();

    @MessageMapping("/session/{sessionId}/join")
    public void joinSession(@DestinationVariable Long sessionId, SessionJoinMessage message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("세션 참가 요청: sessionId={}, userId={}, userName={}", sessionId, message.getUserId(), message.getUserName());
        
        String sessionIdStr = String.valueOf(sessionId);
        sessionParticipants.computeIfAbsent(sessionIdStr, k -> new CopyOnWriteArraySet<>()).add(message.getUserName());
        
        Set<String> participants = sessionParticipants.get(sessionIdStr);
        log.info("현재 참가자: {}", participants);
        
        SessionStatusMessage statusMessage = SessionStatusMessage.builder()
            .action("JOIN")
            .userName(message.getUserName())
            .participants(new ArrayList<>(participants))
            .questionCount(questionRepository.countBySessionId(sessionId).intValue())
            .answerCount(answerRepository.countByQuestionSessionId(sessionId).intValue())
            .build();
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", statusMessage);
        
        Map<String, Object> userMapping = new HashMap<>();
        userMapping.put("userId", message.getUserId());
        userMapping.put("userName", message.getUserName());
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/user-mapping", userMapping);
    }

    @MessageMapping("/session/{sessionId}/leave")
    public void leaveSession(@DestinationVariable Long sessionId, SessionJoinMessage message) {
        log.info("세션 퇴장 요청: sessionId={}, userName={}", sessionId, message.getUserName());
        
        String sessionIdStr = String.valueOf(sessionId);
        Set<String> participants = sessionParticipants.get(sessionIdStr);
        
        if (participants != null) {
            participants.remove(message.getUserName());
            
            SessionStatusMessage statusMessage = SessionStatusMessage.builder()
                .action("LEAVE")
                .userName(message.getUserName())
                .participants(new ArrayList<>(participants))
                .build();
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", statusMessage);
        }
    }

    @MessageMapping("/session/{sessionId}/question")
    @Transactional
    public void handleQuestion(@DestinationVariable Long sessionId, QuestionMessage message) {
        log.info("질문 수신: sessionId={}, text={}", sessionId, message.getText());
        
        try {
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
            
            Question question = Question.builder()
                .session(session)
                .text(message.getText())
                .orderNo(message.getOrderNo())
                .createdAt(LocalDateTime.now())
                .build();
            
            question = questionRepository.save(question);
            log.info("질문 저장 완료: questionId={}", question.getId());
            
            Map<String, Object> responseMessage = new HashMap<>();
            responseMessage.put("questionId", question.getId());
            responseMessage.put("text", question.getText());
            responseMessage.put("orderNo", question.getOrderNo());
            responseMessage.put("timerSeconds", message.getTimerSeconds());
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/question", responseMessage);
            log.info("질문 브로드캐스트 완료");
            
        } catch (Exception e) {
            log.error("질문 처리 중 오류 발생", e);
        }
    }

    @MessageMapping("/session/{sessionId}/answer")
    @Transactional
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage message) {
        log.info("답변 수신: sessionId={}, questionId={}, userId={}", 
            sessionId, message.getQuestionId(), message.getUserId());
        
        try {
            if (message.getQuestionId() == null) {
                log.error("❌ questionId가 null입니다!");
                return;
            }
            
            Question question = questionRepository.findById(message.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found: " + message.getQuestionId()));
            
            User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Answer answer = Answer.builder()
                .question(question)
                .user(user)
                .answerText(message.getAnswerText())
                .createdAt(LocalDateTime.now())
                .build();
            
            answer = answerRepository.save(answer);
            log.info("답변 저장 완료: answerId={}", answer.getId());
            
            Map<String, Object> responseMessage = new HashMap<>();
            responseMessage.put("answerId", answer.getId());
            responseMessage.put("questionId", question.getId());
            responseMessage.put("userId", user.getId());
            responseMessage.put("userName", user.getName());
            responseMessage.put("answerText", answer.getAnswerText());
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", responseMessage);
            log.info("답변 브로드캐스트 완료");
            
        } catch (Exception e) {
            log.error("답변 처리 중 오류 발생", e);
        }
    }

    @MessageMapping("/session/{sessionId}/interviewer-feedback")
    @Transactional
    public void handleInterviewerFeedback(@DestinationVariable Long sessionId, InterviewerFeedbackMessage message) {
        log.info("면접관 피드백 수신: answerId={}, reviewerId={}", 
            message.getAnswerId(), message.getReviewerId());
        
        try {
            Answer answer = answerRepository.findById(message.getAnswerId())
                .orElseThrow(() -> new RuntimeException("Answer not found"));
            
            User reviewer = userRepository.findById(message.getReviewerId())
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));
            
            Feedback feedback = Feedback.builder()
                .answer(answer)
                .reviewer(reviewer)
                .feedbackType(Feedback.FeedbackType.INTERVIEWER)
                .score(message.getScore())
                .improvementSuggestions(message.getComment())
                .createdAt(LocalDateTime.now())
                .build();
            
            feedback = feedbackRepository.save(feedback);
            log.info("면접관 피드백 저장 완료: feedbackId={}", feedback.getId());
            
            Map<String, Object> responseMessage = new HashMap<>();
            responseMessage.put("feedbackId", feedback.getId());
            responseMessage.put("answerId", answer.getId());
            responseMessage.put("reviewerId", reviewer.getId());
            responseMessage.put("reviewerName", reviewer.getName());
            responseMessage.put("score", feedback.getScore());
            responseMessage.put("comment", feedback.getImprovementSuggestions());
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/interviewer-feedback", responseMessage);
            log.info("면접관 피드백 브로드캐스트 완료");
            
        } catch (Exception e) {
            log.error("면접관 피드백 처리 중 오류 발생", e);
        }
    }

    @MessageMapping("/session/{sessionId}/control")
    @Transactional
    public void handleControl(@DestinationVariable Long sessionId, Map<String, Object> message) {
        log.info("제어 메시지 수신: sessionId={}, action={}", sessionId, message.get("action"));
        
        try {
            String action = (String) message.get("action");
            
            Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
            
            if ("START".equals(action)) {
                session.setSessionStatus(Session.SessionStatus.RUNNING);
                session.setStartTime(LocalDateTime.now());
            } else if ("END".equals(action)) {
                session.setSessionStatus(Session.SessionStatus.ENDED);
                session.setEndTime(LocalDateTime.now());
            }
            
            sessionRepository.save(session);
            
            message.put("status", session.getSessionStatus().toString());
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/control", message);
            log.info("제어 메시지 브로드캐스트 완료");
            
        } catch (Exception e) {
            log.error("제어 메시지 처리 중 오류 발생", e);
        }
    }
}