package com.mydiet.controller;

import com.mydiet.model.User;
import com.mydiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, Object> request) {
        try {
            User user = new User();
            user.setNickname((String) request.get("nickname"));
            user.setEmail((String) request.get("email"));
            user.setWeightGoal(Double.parseDouble(request.get("targetWeight").toString()));
            user.setEmotionMode("무자비"); // 기본값
            user.setCreatedAt(LocalDateTime.now());
            
            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "userId", savedUser.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/auth/check")
    public ResponseEntity<?> checkAuth() {
        return ResponseEntity.ok(Map.of("authenticated", true));
    }
}
