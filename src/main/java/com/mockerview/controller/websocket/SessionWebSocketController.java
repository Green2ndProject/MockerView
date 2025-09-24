package com.mockerview.controller.websocket;

import com.mockerview.dto.*;
import com.mockerview.dto.InterviewerFeedbackMessage;
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
            log.info("=== Received question for session {}: {} ===", sessionId, message.getQuestionText());
            
            Long questionId = sessionService.saveQuestion(sessionId, message.getQuestionText(), message.getOrderNo());
            
            message.setQuestionId(questionId);
            message.setTimestamp(LocalDateTime.now());
            
            log.info("=== Broadcasting question to /topic/session/{}/question ===", sessionId);
            
            return message;
        } catch (Exception e) {
            log.error("Error handling question: ", e);
            throw new RuntimeException("질문 처리 실패", e);
        }
    }

    @MessageMapping("/session/{sessionId}/answer")
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage message) {
        try {
            log.info("New answer for session {} from user {}", sessionId, message.getUserName());
            message.setTimestamp(LocalDateTime.now());
            
            Long answerId = sessionService.saveAnswer(message);
            message.setAnswerId(answerId);
            
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/answer", 
                message
            );
            
            aiFeedbackService.generateFeedbackAsync(answerId, sessionId);
            
        } catch (Exception e) {
            log.error("Error handling answer: ", e);
        }
    }

    @MessageMapping("/session/{sessionId}/interviewer-feedback")
    public void handleInterviewerFeedback(@DestinationVariable Long sessionId, InterviewerFeedbackMessage message) {
        try {
            log.info("Interviewer feedback for session {} from reviewer {}", sessionId, message.getReviewerName());
            message.setSessionId(sessionId);
            
            interviewerFeedbackService.submitInterviewerFeedback(message);
            
        } catch (Exception e) {
            log.error("Error handling interviewer feedback: ", e);
        }
    }
}