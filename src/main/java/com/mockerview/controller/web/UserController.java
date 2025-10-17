package com.mockerview.controller.web;

import com.mockerview.dto.FindUsernameRequest;
import com.mockerview.dto.RegisterDTO;
import com.mockerview.dto.ResetPasswordRequest;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/auth")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginForm() {
        log.info("로그인폼 controller 진입 성공!");
        return "user/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "user/register";
    }

    @PostMapping("/register")
    @ResponseBody
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO) {
        
        log.info("회원가입 요청: username={}, email={}, role={}", 
            registerDTO.getUsername(), 
            registerDTO.getEmail(), 
            registerDTO.getRole());
        
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String name = registerDTO.getName();
        String email = registerDTO.getEmail();
        String role = registerDTO.getRole();

        if (username == null || password == null || name == null || email == null || role == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "모든 필드를 입력해주세요."));
        }

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "이미 사용 중인 이메일입니다."));
        }
        
        try {
            User user = User.builder()
                            .username(username)
                            .password(passwordEncoder.encode(password))
                            .name(name)
                            .email(email)
                            .role(User.UserRole.valueOf(role))
                            .build();
            
            userRepository.save(user);
            log.info("회원가입 성공: {}", username);
            
            return ResponseEntity.ok()
                .body(Map.of("message", "회원가입이 완료되었습니다."));
        } catch (Exception e) {
            log.error("회원가입 실패: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "회원가입 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/find-username")
    public String findUsernamePage() {
        return "user/find-username";
    }

    @PostMapping("/find-username")
    @ResponseBody
    public ResponseEntity<?> findUsername(@RequestBody FindUsernameRequest request) {
        try {
            String username = userService.findUsername(request.getName(), request.getEmail());
            return ResponseEntity.ok()
                .body(Map.of("username", username, "message", "아이디를 찾았습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "user/reset-password";
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getUsername(), request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok()
                .body(Map.of("message", "비밀번호가 재설정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "user/withdraw";
    }
}
