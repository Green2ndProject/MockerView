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
        System.out.println("=== 회원가입 요청 받음 ===");
        System.out.println("Request: " + request);
        
        try {
            User user = new User();
            user.setNickname((String) request.get("nickname"));
            user.setEmail((String) request.get("email"));
            user.setWeightGoal(Double.parseDouble(request.get("targetWeight").toString()));
            user.setEmotionMode("무자비"); // 기본값
            user.setCreatedAt(LocalDateTime.now());
            
            System.out.println("사용자 생성 중: " + user.getNickname());
            
            User savedUser = userRepository.save(user);
            System.out.println("사용자 저장 완료 - ID: " + savedUser.getId());
            
            return ResponseEntity.ok(Map.of("success", true, "userId", savedUser.getId()));
        } catch (Exception e) {
            System.err.println("회원가입 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/auth/check")
    public ResponseEntity<?> checkAuth() {
        System.out.println("=== 인증 체크 요청 받음 ===");
        return ResponseEntity.ok(Map.of("authenticated", true));
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        System.out.println("=== 테스트 요청 받음 ===");
        return ResponseEntity.ok("API 서버 정상 작동");
    }
}
