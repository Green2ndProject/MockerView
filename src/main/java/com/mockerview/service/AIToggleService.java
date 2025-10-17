package com.mockerview.service;

import com.mockerview.entity.Session;
import com.mockerview.entity.Answer;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIToggleService {
    
    private final SessionRepository sessionRepository;
    private final AnswerRepository answerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public void toggleAI(Long sessionId, Boolean enabled, String username) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
        
        session.setAiEnabled(enabled);
        sessionRepository.save(session);
        
        messagingTemplate.convertAndSend(
            "/topic/session/" + sessionId,
            Map.of(
                "type", "AI_TOGGLE",
                "enabled", enabled,
                "message", enabled ? "AI 피드백이 활성화되었습니다" : "AI 피드백이 비활성화되었습니다",
                "changedBy", username
            )
        );
    }
    
    @Transactional
    public void updateAiMode(Long sessionId, String mode, String username) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다"));
        
        session.setAiMode(mode);
        sessionRepository.save(session);
        
        messagingTemplate.convertAndSend(
            "/topic/session/" + sessionId,
            Map.of(
                "type", "AI_MODE_CHANGE",
                "mode", mode,
                "message", getAiModeDescription(mode)
            )
        );
    }
    
    private String getAiModeDescription(String mode) {
        return switch(mode) {
            case "OFF" -> "AI 피드백이 완전히 비활성화되었습니다";
            case "BASIC" -> "AI가 기본 평가만 제공합니다";
            case "FULL" -> "AI가 상세한 STAR 분석을 제공합니다";
            case "CUSTOM" -> "사용자 정의 AI 설정이 적용되었습니다";
            default -> "AI 모드가 변경되었습니다";
        };
    }
}
