package com.mockerview.controller.api;

import com.mockerview.dto.PushSubscriptionDTO;
import com.mockerview.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
