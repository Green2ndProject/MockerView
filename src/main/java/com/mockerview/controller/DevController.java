package com.mockerview.controller;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevController {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @GetMapping("/create-admin")
    public String createAdmin() {
        try {
            if (userRepository.findByUsername("mockerview").isPresent()) {
                return "관리자 계정이 이미 존재합니다.";
            }
            
            User admin = User.builder()
                .username("mockerview")
                .password(passwordEncoder.encode("mockerview"))
                .name("관리자")
                .email("admin@mockerview.com")
                .role(User.UserRole.ADMIN)
                .build();
            
            userRepository.save(admin);
            
            log.info("관리자 계정 생성 완료 - username: mockerview, password: mockerview");
            
            return "관리자 계정 생성 완료! (username: mockerview, password: mockerview, email: admin@mockerview.com)";
        } catch (Exception e) {
            log.error("관리자 계정 생성 실패", e);
            return "관리자 계정 생성 실패: " + e.getMessage();
        }
    }
}
