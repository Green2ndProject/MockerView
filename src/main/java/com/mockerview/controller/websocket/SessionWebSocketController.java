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
    public QuestionMessage handleQuestion(@DestinationVariable Long sessionId, QuestionMessage message) {
        try {
            log.info("질문 수신 - sessionId: {}, 질문: {}", sessionId, message.getQuestionText());
            
            Long questionId = sessionService.saveQuestion(sessionId, message.getQuestionText(), message.getOrderNo());
            
            message.setQuestionId(questionId);
            message.setTimestamp(LocalDateTime.now());
            
            log.info("질문 저장 완료 - questionId: {}", questionId);
            return message;
            
        } catch (Exception e) {
            log.error("질문 처리 오류: ", e);
            throw new RuntimeException("질문 처리 실패", e);
        }
    }

    @MessageMapping("/session/{sessionId}/answer")
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage message) {
        try {
            log.info("답변 수신 - sessionId: {}, 사용자: {}", sessionId, message.getUserName());
            message.setTimestamp(LocalDateTime.now());
            
            Long answerId = sessionService.saveAnswer(message);
            message.setAnswerId(answerId);
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/answer", 
                message
            );
            
            aiFeedbackService.generateFeedbackAsync(answerId, sessionId);
            
            log.info("답변 저장 완료 - answerId: {}", answerId);
            
        } catch (Exception e) {
            log.error("답변 처리 오류: ", e);
        }
    }

    @MessageMapping("/session/{sessionId}/interviewer-feedback")
    public void handleInterviewerFeedback(@DestinationVariable Long sessionId, InterviewerFeedbackMessage message) {
        try {
            log.info("면접관 피드백 수신 - sessionId: {}, 리뷰어: {}", sessionId, message.getReviewerName());
            message.setSessionId(sessionId);
            
            interviewerFeedbackService.submitInterviewerFeedback(message);
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/interviewer-feedback",
                message
            );
            
            log.info("면접관 피드백 처리 완료");
            
        } catch (Exception e) {
            log.error("면접관 피드백 처리 오류: ", e);
        }
    }

    @MessageMapping("/session/{sessionId}/join")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleJoin(@DestinationVariable Long sessionId, SessionJoinMessage joinMessage) {
        try {
            log.info("세션 참가 - sessionId: {}, 사용자: {}", sessionId, joinMessage.getUserName());
            
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
            SessionStatusMessage status = sessionService.getSessionStatus(sessionId);
            
            return status;
            
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
            SessionStatusMessage status = sessionService.getSessionStatus(sessionId);
            
            return status;
            
        } catch (Exception e) {
            log.error("세션 종료 처리 오류: ", e);
            return null;
        }
    }
}