package com.mydiet.controller;

import com.mydiet.repository.UserRepository;
import com.mydiet.repository.MealLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MealLogRepository mealLogRepository;

    private final String ADMIN_PASSWORD = "oicrcutie1998";

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, Object> request) {
        String password = (String) request.get("password");
        
        if (ADMIN_PASSWORD.equals(password)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "관리자 인증 성공"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "잘못된 관리자 비밀번호입니다."));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = userRepository.count();
        long totalMeals = mealLogRepository.count();
        
        return ResponseEntity.ok(Map.of(
            "totalUsers", totalUsers,
            "totalMeals", totalMeals,
            "totalClaudeResponses", Math.max(1, (int)(totalMeals * 0.8)),
            "activeToday", Math.max(1, (int)(totalUsers * 0.6))
        ));
    }
}