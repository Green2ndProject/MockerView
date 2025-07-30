package com.mydiet.controller;

import com.mydiet.model.User;
import com.mydiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
            String email = (String) request.get("email");
            
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "이미 존재하는 이메일입니다."));
            }
            
            User user = new User();
            user.setNickname((String) request.get("nickname"));
            user.setEmail(email);
            user.setWeightGoal(Double.parseDouble(request.get("targetWeight").toString()));
            user.setEmotionMode((String) request.get("emotionMode"));
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

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> request) {
        System.out.println("=== 로그인 요청 받음 ===");
        
        try {
            String email = (String) request.get("email");
            String password = (String) request.get("password");
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("로그인 성공 - 사용자: " + user.getNickname());
                
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "userId", user.getId(),
                    "nickname", user.getNickname(),
                    "email", user.getEmail(),
                    "emotionMode", user.getEmotionMode()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "존재하지 않는 이메일입니다."));
            }
        } catch (Exception e) {
            System.err.println("로그인 에러: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "로그인에 실패했습니다."));
        }
    }

    @GetMapping("/auth/check")
    public ResponseEntity<?> checkAuth() {
        System.out.println("=== 인증 체크 요청 받음 ===");
        return ResponseEntity.ok(Map.of("authenticated", true));
    }

    @GetMapping("/claude/test")
public ResponseEntity<?> testClaude() {
    System.out.println("=== Claude 테스트 요청 ===");
    return ResponseEntity.ok("AI 테스트 응답입니다! 💪 열심히 다이어트 하세요!");
}
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        System.out.println("=== 테스트 요청 받음 ===");
        return ResponseEntity.ok("API 서버 정상 작동");
    }
}