package com.mockerview.controller.api;

import com.mockerview.dto.PushSubscriptionDTO;
import com.mockerview.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationController {
    
    private final PushNotificationService pushService;
    
    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(
            Authentication auth,
            @RequestBody PushSubscriptionDTO subscription) {
        try {
            pushService.subscribe(auth.getName(), subscription);
            return ResponseEntity.ok("구독 성공");
        } catch (Exception e) {
            log.error("Push subscription failed", e);
            return ResponseEntity.badRequest().body("구독 실패");
        }
    }
    
    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestBody String endpoint) {
        try {
            pushService.unsubscribe(endpoint);
            return ResponseEntity.ok("구독 해제 성공");
        } catch (Exception e) {
            log.error("Push unsubscribe failed", e);
            return ResponseEntity.badRequest().body("구독 해제 실패");
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> testPush(
            Authentication auth,
            @RequestBody Map<String, Object> request) {
        try {
            String username = auth.getName();
            String title = (String) request.getOrDefault("title", "테스트 알림");
            String body = (String) request.getOrDefault("body", "푸시 알림 테스트입니다");
            String url = (String) request.getOrDefault("url", "/");
            
            log.info("푸시 알림 테스트 요청 - username: {}, title: {}", username, title);
            
            pushService.sendNotificationToUser(username, title, body, url);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "푸시 알림이 전송되었습니다",
                "username", username
            ));
        } catch (Exception e) {
            log.error("푸시 알림 테스트 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "푸시 알림 전송 실패: " + e.getMessage()
            ));
        }
    }
}