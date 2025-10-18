package com.mockerview.controller;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {
    
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    @GetMapping("/hello")
    public ResponseEntity<?> hello() {
        return ResponseEntity.ok("Hello from DevController!");
    }

    @PostMapping("/test-push/{username}")
    public ResponseEntity<?> testPush(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            pushNotificationService.sendNotification(
                user,
                "🎯 테스트 알림",
                "Push 알림이 정상 작동합니다!",
                "/"
            );
            
            return ResponseEntity.ok("Push sent to " + username + "!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
