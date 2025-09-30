package com.mockerview.controller.websocket;

import com.mockerview.dto.*;
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

@Controller
@RequiredArgsConstructor
@Slf4j
public class SessionWebSocketController {
    
    private final SessionService sessionService;
    private final AIFeedbackService aiFeedbackService;
    private final InterviewerFeedbackService interviewerFeedbackService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/session/{sessionId}/question")
    @SendTo("/topic/session/{sessionId}/question")
    public QuestionMessage handleQuestion(@DestinationVariable Long sessionId, 
                                         QuestionMessage message,
                                         SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            log.info("질문 수신 - sessionId: {}, 사용자: {} (ID: {})", sessionId, userName, userId);
            
            Long questionId = sessionService.saveQuestion(sessionId, message.getQuestionText(), message.getOrderNo(), userId);
            
            message.setQuestionId(questionId);
            message.setQuestionerId(userId);
            message.setTimestamp(LocalDateTime.now());
            
            return message;
            
        } catch (Exception e) {
            log.error("질문 처리 오류: ", e);
            throw new RuntimeException("질문 처리 실패", e);
        }
    }

    @MessageMapping("/session/{sessionId}/answer")
    public void handleAnswer(@DestinationVariable Long sessionId, 
                           AnswerMessage message,
                           SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            log.info("답변 수신 - sessionId: {}, 사용자: {} (ID: {})", sessionId, userName, userId);
            
            message.setUserId(userId);
            message.setUserName(userName);
            message.setTimestamp(LocalDateTime.now());
            
            Long answerId = sessionService.saveAnswer(message);
            message.setAnswerId(answerId);
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", message);
            
            aiFeedbackService.generateFeedbackAsync(answerId, sessionId);
            
        } catch (Exception e) {
            log.error("답변 처리 오류: ", e);
        }
    }

    @MessageMapping("/session/{sessionId}/interviewer-feedback")
    public void handleInterviewerFeedback(@DestinationVariable Long sessionId, 
                                            InterviewerFeedbackMessage message,
                                            SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long reviewerId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String reviewerName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            log.info("면접관 피드백 수신 - sessionId: {}, 리뷰어: {} (ID: {})", sessionId, reviewerName, reviewerId);
            
            message.setSessionId(sessionId);
            message.setReviewerId(reviewerId);
            message.setReviewerName(reviewerName);
            
            interviewerFeedbackService.submitInterviewerFeedback(message);
            
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/interviewer-feedback", message);
            
        } catch (Exception e) {
            log.error("면접관 피드백 처리 오류: ", e);
        }
    }

    @MessageMapping("/session/{sessionId}/join")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleJoin(@DestinationVariable Long sessionId, 
                                            SessionJoinMessage joinMessage,
                                            SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            log.info("세션 참가 - sessionId: {}, 사용자: {} (ID: {})", sessionId, userName, userId);
            
            joinMessage.setUserId(userId);
            joinMessage.setUserName(userName);
            
            SessionStatusMessage status = sessionService.getSessionStatus(sessionId);
            
            return status;
            
        } catch (Exception e) {
            log.error("세션 참가 처리 오류: ", e);
            return null;
        }
    }

    @MessageMapping("/session/{sessionId}/start")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleStartSession(@DestinationVariable Long sessionId) {
        try {
            log.info("세션 시작 - sessionId: {}", sessionId);
            sessionService.startSession(sessionId);
            return sessionService.getSessionStatus(sessionId);
        } catch (Exception e) {
            log.error("세션 시작 처리 오류: ", e);
            return null;
        }
    }

    @MessageMapping("/session/{sessionId}/end")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleEndSession(@DestinationVariable Long sessionId) {
        try {
            log.info("세션 종료 - sessionId: {}", sessionId);
            sessionService.endSession(sessionId);
            return sessionService.getSessionStatus(sessionId);
        } catch (Exception e) {
            log.error("세션 종료 처리 오류: ", e);
            return null;
        }
    }
}