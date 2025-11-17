package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                response.put("success", false);
                response.put("message", "현재 비밀번호가 일치하지 않습니다.");
                return ResponseEntity.ok(response);
            }
            
            if (request.getNewPassword().length() < 8) {
                response.put("success", false);
                response.put("message", "새 비밀번호는 8자 이상이어야 합니다.");
                return ResponseEntity.ok(response);
            }
            
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userService.save(user);
            
            log.info("비밀번호 변경 성공: userId={}", user.getId());
            
            response.put("success", true);
            response.put("message", "비밀번호가 변경되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "비밀번호 변경 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
}
