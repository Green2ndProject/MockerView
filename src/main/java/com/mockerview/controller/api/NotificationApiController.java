package com.mockerview.controller.api;

import com.mockerview.dto.PushSubscriptionDTO;
import com.mockerview.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationApiController {
    
    private final PushNotificationService pushNotificationService;
    
    @Value("${vapid.public.key}")
    private String vapidPublicKey;
    
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PushSubscriptionDTO subscription) {
        
        try {
            pushNotificationService.subscribe(userDetails.getUsername(), subscription);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Push subscription failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> body) {
        try {
            pushNotificationService.unsubscribe(body.get("endpoint"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Push unsubscribe failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/vapid-public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidPublicKey));
    }
}
