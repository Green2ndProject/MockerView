package com.mockerview.controller.websocket;

import com.mockerview.dto.*;
import com.mockerview.entity.Session;
import com.mockerview.repository.AnswerRepository;
import com.mockerview.repository.QuestionRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.service.SessionService;
import com.mockerview.service.AIFeedbackService;
import com.mockerview.service.InterviewerFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket을 통한 실시간 면접 세션 처리 컨트롤러
 * 질문 출제, 답변 제출, 피드백 처리 등의 면접 관련 실시간 기능을 담당
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class SessionWebSocketController {
    
    private final SessionService sessionService;
    private final AIFeedbackService aiFeedbackService;
    private final InterviewerFeedbackService interviewerFeedbackService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    
    private final Map<Long, Set<String>> sessionParticipants = new ConcurrentHashMap<>();

    /**
     * 면접 질문을 처리합니다.
     * 질문을 저장하고, questionId, questionerId, timestamp를 설정한 후, 
     * /topic/session/{sessionId}/question으로 브로드캐스팅합니다.
     */
    @MessageMapping("/session/{sessionId}/question")
    @SendTo("/topic/session/{sessionId}/question")
    public QuestionMessage handleQuestion(
            @DestinationVariable Long sessionId, 
            Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            if (sessionId == null) {
                throw new IllegalArgumentException("SessionId가 null입니다");
            }
            
            if (userId == null) {
                throw new IllegalArgumentException("UserId가 null입니다. WebSocket 인증이 실패했습니다.");
            }
            
            String questionText = (String) payload.get("text");
            Integer orderNo = payload.get("orderNo") != null ? ((Number) payload.get("orderNo")).intValue() : 1;
            Integer timerSeconds = payload.get("timerSeconds") != null ? ((Number) payload.get("timerSeconds")).intValue() : 60;
            
            log.info("질문 수신 - sessionId: {}, 사용자: {} (ID: {}), Timer: {}", 
                    sessionId, userName, userId, timerSeconds);
            
            Long questionId = sessionService.saveQuestion(
                sessionId, 
                questionText, 
                orderNo, 
                userId,
                timerSeconds
            );
            
            QuestionMessage message = new QuestionMessage();
            message.setQuestionId(questionId);
            message.setQuestionText(questionText);
            message.setOrderNo(orderNo);
            message.setTimer(timerSeconds);
            message.setQuestionerId(userId);
            message.setTimestamp(LocalDateTime.now());

            SessionStatusMessage statusMessage = sessionService.getSessionStatus(sessionId);
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/status", 
                statusMessage
            );
            
            log.info("질문 저장 완료 - questionId: {}", questionId);
            
            return message;
            
        } catch (Exception e) {
            log.error("질문 처리 오류: ", e);
            throw new RuntimeException("질문 처리 실패", e); 
        }
    }

    /**
     * 면접 답변을 처리합니다.
     * 답변을 저장하고, /topic/session/{sessionId}/answer로 브로드캐스팅한 후,
     * AI 피드백 생성을 비동기적으로 요청합니다.
     */
    @MessageMapping("/session/{sessionId}/answer")
    public void handleAnswer(
            @DestinationVariable Long sessionId, 
            AnswerMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            if (sessionId == null) {
                throw new IllegalArgumentException("SessionId가 null입니다");
            }
            
            if (userId == null) {
                throw new IllegalArgumentException("UserId가 null입니다. WebSocket 인증이 실패했습니다.");
            }
            
            log.info("답변 수신 - sessionId: {}, 사용자: {} (ID: {})", sessionId, userName, userId);
            
            message.setUserId(userId);
            message.setUserName(userName);
            message.setTimestamp(LocalDateTime.now());
            
            Long answerId = sessionService.saveAnswer(message);
            message.setAnswerId(answerId);
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", message);
            
            aiFeedbackService.generateFeedbackAsync(answerId, sessionId);
            
            log.info("답변 저장 및 AI 피드백 요청 완료 - answerId: {}", answerId);
            
        } catch (Exception e) {
            log.error("답변 처리 오류: ", e);
        }
    }

    /**
     * 면접관 피드백을 처리하고 브로드캐스팅합니다.
     */
    @MessageMapping("/session/{sessionId}/interviewer-feedback")
    public void handleInterviewerFeedback(@DestinationVariable Long sessionId, InterviewerFeedbackMessage request) {
        log.info("면접관 피드백 수신 - sessionId: {}, 리뷰어: {} (ID: {})", 
                sessionId, request.getReviewerName(), request.getReviewerId());
        
        interviewerFeedbackService.submitInterviewerFeedback(request);
        
        messagingTemplate.convertAndSend(
            "/topic/session/" + sessionId + "/interviewer-feedback", 
            request
        );
    
        log.info("면접관 피드백 처리 완료");
    }
    
    /**
     * 세션 참가를 처리하고 현재 세션 상태를 반환(브로드캐스팅)합니다.
     */
    @MessageMapping("/session/{sessionId}/join")
    public void handleJoin(@DestinationVariable Long sessionId, SessionStatusMessage message) {
        log.info("참가 요청: sessionId={}, userName={}", sessionId, message.getUserName());
        
        sessionParticipants.computeIfAbsent(sessionId, k -> new CopyOnWriteArraySet<>())
            .add(message.getUserName());
        
        Session session = sessionRepository.findById(sessionId).orElse(null);
        Long questionCount = questionRepository.countBySessionId(sessionId);
        Long answerCount = answerRepository.countBySessionId(sessionId);
        
        SessionStatusMessage statusMessage = SessionStatusMessage.builder()
            .sessionId(sessionId)
            .status(session != null ? session.getStatus().name() : "WAITING")
            .questionCount(questionCount.intValue())
            .answerCount(answerCount.intValue())
            .participants(new ArrayList<>(sessionParticipants.get(sessionId)))
            .action("JOIN")
            .userName(message.getUserName())
            .timestamp(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/status", statusMessage);
        log.info("참가 완료: {}, 현재 참가자: {}", message.getUserName(), statusMessage.getParticipants());
    }

    /**
     * 세션 시작을 처리하고 변경된 세션 상태를 브로드캐스팅합니다.
     */
    @MessageMapping("/session/{sessionId}/start")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleStartSession(@DestinationVariable Long sessionId) {
        try {
            log.info("세션 시작 - sessionId: {}", sessionId);
            
            sessionService.startSession(sessionId);
            
            SessionStatusMessage status = sessionService.getSessionStatus(sessionId);
            
            return status;
            
        } catch (Exception e) {
            log.error("세션 시작 처리 오류: ", e);
            return null;
        }
    }

    /**
     * 세션 종료를 처리하고 변경된 세션 상태를 브로드캐스팅합니다.
     */
    @MessageMapping("/session/{sessionId}/end")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleEndSession(@DestinationVariable Long sessionId) {
        try {
            log.info("세션 종료 - sessionId: {}", sessionId);
            
            sessionService.endSession(sessionId);
            
            SessionStatusMessage status = sessionService.getSessionStatus(sessionId);
            
            return status;
            
        } catch (Exception e) {
            log.error("세션 종료 처리 오류: ", e);
            return null;
        }
    }

    @MessageMapping("/session/{sessionId}/control")
    public void handleControl(
            @DestinationVariable Long sessionId,
            Map<String, Object> message,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            String action = (String) message.get("action");
            log.info("제어 메시지 수신 - sessionId: {}, action: {}", sessionId, action);
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/control",
                message
            );
            
            log.info("제어 메시지 브로드캐스트 완료");
            
        } catch (Exception e) {
            log.error("제어 메시지 처리 오류: ", e);
        }
    }
}