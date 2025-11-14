package com.mockerview.service;

import com.mockerview.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendVoiceAnalysisComplete(Long userId, Long answerId) {
        NotificationMessage notification = NotificationMessage.builder()
            .type("VOICE_ANALYSIS_COMPLETE")
            .title("🎤 음성 분석 완료")
            .message("음성 분석이 완료되었습니다. 결과를 확인해보세요!")
            .link("/analysis/voice/result")
            .timestamp(LocalDateTime.now())
            .data(answerId)
            .build();
        
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
        log.info("✅ 음성 분석 완료 알림 전송 - userId: {}, answerId: {}", userId, answerId);
    }
    
    public void sendFacialAnalysisComplete(Long userId, Long answerId) {
        NotificationMessage notification = NotificationMessage.builder()
            .type("FACIAL_ANALYSIS_COMPLETE")
            .title("😊 표정 분석 완료")
            .message("표정 분석이 완료되었습니다. 결과를 확인해보세요!")
            .link("/analysis/facial/result")
            .timestamp(LocalDateTime.now())
            .data(answerId)
            .build();
        
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
        log.info("✅ 표정 분석 완료 알림 전송 - userId: {}, answerId: {}", userId, answerId);
    }
    
    public void sendMBTIAnalysisComplete(Long userId) {
        NotificationMessage notification = NotificationMessage.builder()
            .type("MBTI_ANALYSIS_COMPLETE")
            .title("🧠 면접 MBTI 분석 완료")
            .message("당신의 면접 스타일 유형이 분석되었습니다!")
            .link("/analysis/mbti/result")
            .timestamp(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
        log.info("✅ MBTI 분석 완료 알림 전송 - userId: {}", userId);
    }

    public void sendMBTIAnalysisAvailable(Long userId) {
        NotificationMessage notification = NotificationMessage.builder()
            .type("MBTI_ANALYSIS_AVAILABLE")
            .title("🎉 MBTI 분석 가능!")
            .message("5개 이상의 답변이 모였습니다. 이제 면접 MBTI를 분석할 수 있습니다!")
            .link("/auth/mypage/stats")
            .timestamp(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
        log.info("✅ MBTI 분석 가능 알림 전송 - userId: {}", userId);
    }
    
    public void sendFeedbackReceived(Long userId, Long answerId, String feedbackType) {
        NotificationMessage notification = NotificationMessage.builder()
            .type("FEEDBACK_RECEIVED")
            .title("💬 새로운 피드백")
            .message(feedbackType + " 피드백을 받았습니다!")
            .link("/auth/mypage/stats")
            .timestamp(LocalDateTime.now())
            .data(answerId)
            .build();
        
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
        log.info("✅ 피드백 수신 알림 전송 - userId: {}, answerId: {}", userId, answerId);
    }
    
    public void sendSessionStarted(Long sessionId, String sessionTitle) {
        NotificationMessage notification = NotificationMessage.builder()
            .type("SESSION_STARTED")
            .title("🎯 세션 시작")
            .message(sessionTitle + " 세션이 시작되었습니다!")
            .link("/session/" + sessionId)
            .timestamp(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/notifications", notification);
        log.info("✅ 세션 시작 알림 전송 - sessionId: {}", sessionId);
    }
}