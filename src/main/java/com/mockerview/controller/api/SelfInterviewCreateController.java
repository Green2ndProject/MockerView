package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.Session;
import com.mockerview.service.SelfInterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/selfinterview")
@RequiredArgsConstructor
@Slf4j
public class SelfInterviewCreateController {

    private final SelfInterviewService selfInterviewService;

    @PostMapping(value = "/create-with-ai", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createSelfInterview(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            log.info("📥 셀프면접 생성 요청: {}", payload);
            
            String title = payload.get("title").toString();
            String sessionType = payload.get("sessionType").toString();
            String difficulty = payload.get("difficulty").toString();
            String category = payload.get("category").toString();
            int questionCount = Integer.parseInt(payload.get("questionCount").toString());
            
            log.info("셀프면접 생성 - userId: {}, title: {}, type: {}, difficulty: {}, category: {}, count: {}", 
                    userDetails.getUserId(), title, sessionType, difficulty, category, questionCount);
            
            Session session = selfInterviewService.createSelfInterviewSession(
                userDetails.getUserId(),
                title,
                sessionType, 
                difficulty, 
                category, 
                questionCount
            );
            
            log.info("✅ 셀프면접 생성 완료 - sessionId: {}", session.getId());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "success", true,
                    "sessionId", session.getId(),
                    "redirectUrl", "/selfinterview/room/" + session.getId()
                ));
            
        } catch (Exception e) {
            log.error("❌ 셀프면접 생성 실패", e);
            
            String errorMessage = e.getMessage();
            boolean isLimitExceeded = errorMessage != null && errorMessage.contains("SESSION_LIMIT_EXCEEDED");
            
            return ResponseEntity.internalServerError()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "success", false,
                    "limitExceeded", isLimitExceeded,
                    "message", isLimitExceeded ? "세션 생성 한도에 도달했습니다." : errorMessage
                ));
        }
    }
}