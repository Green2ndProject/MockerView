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

    /**
     * 면접 질문을 처리합니다.
     * 질문을 저장하고, questionId, questionerId, timestamp를 설정한 후, 
     * /topic/session/{sessionId}/question으로 브로드캐스팅합니다.
     */
    @MessageMapping("/session/{sessionId}/question")
    @SendTo("/topic/session/{sessionId}/question")
    public QuestionMessage handleQuestion(
            @DestinationVariable Long sessionId, 
            QuestionMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 사용자 정보는 세션 속성에서 가져오는 것을 표준화합니다.
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            // sessionId null 체크
            if (sessionId == null) {
                throw new IllegalArgumentException("SessionId가 null입니다");
            }
            
            // userId null 체크 - WebSocket 인증 실패 시 예외 발생
            if (userId == null) {
                throw new IllegalArgumentException("UserId가 null입니다. WebSocket 인증이 실패했습니다.");
            }
            
            log.info("질문 수신 - sessionId: {}, 사용자: {} (ID: {}), Timer: {}", 
                     sessionId, userName, userId, message.getTimer());
            
            // 타이머 값을 포함하여 서비스 레이어에 전달합니다.
            Long questionId = sessionService.saveQuestion(
                sessionId, 
                message.getQuestionText(), 
                message.getOrderNo(), 
                userId, // 세션 속성에서 가져온 userId 사용
                message.getTimer() // 두 번째 버전에서 추가된 timer 값 사용
            );
            
            // 응답 메시지 설정
            message.setQuestionId(questionId);
            message.setQuestionerId(userId); // 질문자 ID 설정
            message.setTimestamp(LocalDateTime.now());
            
            log.info("질문 저장 완료 - questionId: {}", questionId);
            
            return message;
            
        } catch (Exception e) {
            log.error("질문 처리 오류: ", e);
            // 클라이언트에 오류를 알리기 위해 RuntimeException을 던집니다.
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
            // 사용자 정보는 세션 속성에서 가져오는 것을 표준화합니다.
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            // sessionId null 체크
            if (sessionId == null) {
                throw new IllegalArgumentException("SessionId가 null입니다");
            }
            
            // userId null 체크 - WebSocket 인증 실패 시 예외 발생
            if (userId == null) {
                throw new IllegalArgumentException("UserId가 null입니다. WebSocket 인증이 실패했습니다.");
            }
            
            log.info("답변 수신 - sessionId: {}, 사용자: {} (ID: {})", sessionId, userName, userId);
            
            // 답변 메시지에 사용자 정보 설정
            message.setUserId(userId);
            message.setUserName(userName);
            message.setTimestamp(LocalDateTime.now());
            
            // 답변 저장
            Long answerId = sessionService.saveAnswer(message);
            message.setAnswerId(answerId);
            
            // 답변 메시지를 해당 세션 토픽에 브로드캐스팅
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/answer", message);
            
            // AI 피드백 비동기 요청
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
    public void handleInterviewerFeedback(
            @DestinationVariable Long sessionId, 
            InterviewerFeedbackMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 리뷰어 정보는 세션 속성에서 가져오는 것을 표준화합니다.
            Long reviewerId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String reviewerName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            // sessionId null 체크
            if (sessionId == null) {
                throw new IllegalArgumentException("SessionId가 null입니다");
            }
            
            // reviewerId null 체크 - WebSocket 인증 실패 시 예외 발생
            if (reviewerId == null) {
                throw new IllegalArgumentException("ReviewerId가 null입니다. WebSocket 인증이 실패했습니다.");
            }
            
            log.info("면접관 피드백 수신 - sessionId: {}, 리뷰어: {} (ID: {})", sessionId, reviewerName, reviewerId);
            
            // 피드백 메시지에 리뷰어 정보 설정
            message.setSessionId(sessionId);
            message.setReviewerId(reviewerId);
            message.setReviewerName(reviewerName); // 세션 속성에서 가져온 이름으로 설정
            
            // 면접관 피드백 저장
            interviewerFeedbackService.submitInterviewerFeedback(message);
            
            // 피드백 메시지를 해당 세션 토픽에 브로드캐스팅
            messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/interviewer-feedback", message);
            
            log.info("면접관 피드백 처리 완료");
            
        } catch (Exception e) {
            log.error("면접관 피드백 처리 오류: ", e);
        }
    }

    /**
     * 세션 참가를 처리하고 현재 세션 상태를 반환(브로드캐스팅)합니다.
     */
    @MessageMapping("/session/{sessionId}/join")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleJoin(
            @DestinationVariable Long sessionId, 
            SessionJoinMessage joinMessage,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 사용자 정보는 세션 속성에서 가져오는 것을 표준화합니다.
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String userName = (String) headerAccessor.getSessionAttributes().get("userName");
            
            // sessionId 확인 (URL 파라미터 또는 메시지에서)
            Long actualSessionId = sessionId != null ? sessionId : joinMessage.getSessionId();
            
            log.info("세션 참가 - sessionId: {}, 사용자: {} (ID: {})", actualSessionId, userName, userId);
            
            // joinMessage 객체에 사용자 정보를 설정 (필요하다면)
            joinMessage.setUserId(userId);
            joinMessage.setUserName(userName);
            
            // 세션 상태를 가져와 브로드캐스팅
            SessionStatusMessage status = sessionService.getSessionStatus(actualSessionId);
            
            return status;
            
        } catch (Exception e) {
            log.error("세션 참가 처리 오류: ", e);
            return null;
        }
    }

    /**
     * 세션 시작을 처리하고 변경된 세션 상태를 브로드캐스팅합니다.
     */
    @MessageMapping("/session/{sessionId}/start")
    @SendTo("/topic/session/{sessionId}/status")
    public SessionStatusMessage handleStartSession(@DestinationVariable Long sessionId) {
        try {
            log.info("세션 시작 - sessionId: {}", sessionId);
            
            // 세션 상태를 RUNNING으로 변경
            sessionService.startSession(sessionId);
            
            // 변경된 세션 상태를 가져와 브로드캐스팅
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
            
            // 세션 상태를 ENDED로 변경
            sessionService.endSession(sessionId);
            
            // 변경된 세션 상태를 가져와 브로드캐스팅
            SessionStatusMessage status = sessionService.getSessionStatus(sessionId);
            
            return status;
            
        } catch (Exception e) {
            log.error("세션 종료 처리 오류: ", e);
            return null;
        }
    }
}