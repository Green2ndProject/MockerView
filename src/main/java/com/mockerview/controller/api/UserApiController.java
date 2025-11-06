package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {

        private final UserService userService;
        private final UserRepository userRepository;
        private final BCryptPasswordEncoder passwordEncoder;

        @GetMapping("/me")
        public ResponseEntity<Map<String, Object>> getCurrentUser(
                @AuthenticationPrincipal CustomUserDetails userDetails) {
                try {
                User user = userRepository.findByUsername(userDetails.getUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("name", user.getName());
                response.put("email", user.getEmail());
                response.put("role", user.getRole());
                
                log.info("✅ 현재 사용자 정보 조회 - userId: {}, username: {}", user.getId(), user.getUsername());
                
                return ResponseEntity.ok(response);
                } catch (Exception e) {
                log.error("❌ 현재 사용자 정보 조회 실패", e);
                return ResponseEntity.status(500).body(Map.of("error", "User info fetch failed"));
                }
        }

        @PostMapping("/change-password")
        public ResponseEntity<?> changePassword(
                @AuthenticationPrincipal UserDetails userDetails,
                @RequestBody Map<String, String> request) {
                
                try {
                String username = userDetails.getUsername();
                String currentPassword = request.get("currentPassword");
                String newPassword = request.get("newPassword");
                
                log.info("🔐 비밀번호 변경 요청 - username: {}", username);
                
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
                
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        log.warn("❌ 현재 비밀번호 불일치 - username: {}", username);
                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "success", false,
                                        "message", "현재 비밀번호가 일치하지 않습니다."
                                ));
                }
                
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                
                log.info("✅ 비밀번호 변경 완료 - username: {}", username);
                
                return ResponseEntity.ok()
                        .body(Map.of(
                                "success", true,
                                "message", "비밀번호가 변경되었습니다."
                        ));
                
                } catch (Exception e) {
                log.error("❌ 비밀번호 변경 실패", e);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "비밀번호 변경 중 오류가 발생했습니다."
                        ));
                }
        }

        @PostMapping("/withdraw")
        public ResponseEntity<?> withdrawUser(
                @AuthenticationPrincipal UserDetails userDetails,
                @RequestBody Map<String, String> request) {
                
                try {
                String username = userDetails.getUsername();
                String reason = request.get("reason");
                
                log.info("🚪 회원 탈퇴 요청 - username: {}", username);
                
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
                
                user.setIsDeleted(1);
                user.setDeletedAt(LocalDateTime.now());
                user.setWithdrawalReason(reason);
                userRepository.save(user);
                
                log.info("✅ 회원 탈퇴 완료 - username: {}", username);
                
                return ResponseEntity.ok()
                        .body(Map.of(
                                "success", true,
                                "message", "회원 탈퇴가 완료되었습니다.",
                                "redirect", "/auth/logout"
                        ));
                
                } catch (Exception e) {
                log.error("❌ 회원 탈퇴 실패", e);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "회원 탈퇴 중 오류가 발생했습니다."
                        ));
                }
        }

        @GetMapping("/{userId}/role")
        public ResponseEntity<?> getUserRole(@PathVariable Long userId) {
                return userRepository.findById(userId)
                        .map(user -> ResponseEntity.ok(Map.of(
                                "role", user.getRole().name(),
                                "userId", userId
                        )))
                        .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/{userId}/name")
        public ResponseEntity<?> getUserName(@PathVariable Long userId) {
                return userRepository.findById(userId)
                        .map(user -> ResponseEntity.ok(Map.of(
                                "name", user.getName(),
                                "userId", userId
                        )))
                        .orElse(ResponseEntity.notFound().build());
        }
}