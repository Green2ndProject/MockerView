package com.mockerview.controller.api;

import com.mockerview.dto.AgoraTokenDTO;
import com.mockerview.service.AgoraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mockerview.dto.CustomUserDetails;

@Slf4j
@RestController
@RequestMapping("/api/agora")
@RequiredArgsConstructor
public class AgoraApiController {

    private final AgoraService agoraService;

    @PostMapping("/token/{sessionId}")
    public ResponseEntity<?> getToken(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (sessionId == null) {
                log.error("세션 ID가 null입니다");
                return ResponseEntity.badRequest()
                    .body("Invalid session ID");
            }
            
            Long userId = userDetails.getUserId();
            log.info("Agora 토큰 요청 - sessionId: {}, userId: {}", sessionId, userId);
            
            AgoraTokenDTO token = agoraService.generateTokenForSession(sessionId, userId);
            
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("Agora 토큰 생성 실패 - sessionId: {}", sessionId, e);
            return ResponseEntity.status(500)
                .body("Failed to generate token: " + e.getMessage());
        }
    }
    
    @GetMapping("/validate/{sessionId}")
    public ResponseEntity<?> validateSession(@PathVariable Long sessionId) {
        try {
            if (sessionId == null) {
                return ResponseEntity.ok()
                    .body(new ValidationResponse(false, "Invalid session ID"));
            }
            
            return ResponseEntity.ok()
                .body(new ValidationResponse(true, "Valid session"));
        } catch (Exception e) {
            log.error("세션 검증 실패", e);
            return ResponseEntity.status(500)
                .body(new ValidationResponse(false, e.getMessage()));
        }
    }
    
    private static class ValidationResponse {
        public boolean valid;
        public String message;
        
        public ValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}
