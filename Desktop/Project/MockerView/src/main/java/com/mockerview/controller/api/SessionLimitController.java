package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.SessionLimitResponseDTO;
import com.mockerview.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/subscription", "/api/session-limit"})
@RequiredArgsConstructor
@Slf4j
public class SessionLimitController {
    
    private final SubscriptionService subscriptionService;
    
    @GetMapping("/check")
    public ResponseEntity<SessionLimitResponseDTO> checkSessionLimit(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Long userId = userDetails.getUserId();
            log.info("세션 제한 체크 요청 - userId: {}", userId);
            
            Map<String, Object> limitInfo = subscriptionService.getSessionLimitInfo(userId);
            
            SessionLimitResponseDTO response = SessionLimitResponseDTO.builder()
                .limitReached((Boolean) limitInfo.get("limitReached"))
                .canCreate((Boolean) limitInfo.get("canCreate"))
                .usedSessions((Integer) limitInfo.get("usedSessions"))
                .sessionLimit((Integer) limitInfo.get("sessionLimit"))
                .currentPlan((String) limitInfo.get("currentPlan"))
                .message((String) limitInfo.get("message"))
                .upgradeUrl("/payment/plans")
                .build();
            
            log.info("세션 제한 응답 - userId: {}, limitReached: {}, used: {}/{}", 
                userId, response.getLimitReached(), response.getUsedSessions(), response.getSessionLimit());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("세션 제한 체크 실패", e);
            
            SessionLimitResponseDTO errorResponse = SessionLimitResponseDTO.builder()
                .limitReached(true)
                .canCreate(false)
                .usedSessions(0)
                .sessionLimit(0)
                .currentPlan("ERROR")
                .message("세션 제한 정보를 불러올 수 없습니다.")
                .upgradeUrl("/payment/plans")
                .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    @GetMapping("/review-read-info")
    public ResponseEntity<Map<String, Object>> getReviewReadInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            Long userId = userDetails.getUserId();
            log.info("리뷰 읽기 정보 요청 - userId: {}", userId);
            
            Map<String, Object> info = subscriptionService.getReviewReadInfo(userId);
            
            log.info("리뷰 읽기 정보 응답 - userId: {}, canRead: {}, used: {}/{}", 
                userId, info.get("canRead"), info.get("usedReads"), info.get("readLimit"));
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            log.error("리뷰 읽기 정보 조회 실패", e);
            
            Map<String, Object> errorResponse = Map.of(
                "canRead", false,
                "limitReached", true,
                "usedReads", 0,
                "readLimit", 0,
                "currentPlan", "ERROR",
                "message", "정보를 불러올 수 없습니다."
            );
            
            return ResponseEntity.ok(errorResponse);
        }
    }
}