package com.mockerview.service;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
@Service
public class UserService {

    
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public void withdraw(String username, String password, String reason) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        
        log.info("탈퇴 로직 - Service 진입 성공. 사용자: {}", username); 
        log.info("탈퇴 로직 - DB 저장 비밀번호: {}", user.getPassword());
        log.info("탈퇴 로직 - 요청된 비밀번호: {}", password); // 🚨 실제 운영 환경에서는 절대 찍으면 안 됩니다! 진단용입니다.
        log.info("탈퇴 로직 - 비밀번호 검증 시작: {}", username);

        if(!passwordEncoder.matches(password, user.getPassword())){
            log.warn("탈퇴 로직 실패 - 비밀번호 불일치: {}", username);
            throw new IllegalArgumentException("invalid password");
        }

        log.info("탈퇴 로직 성공 - Soft Delete 처리 시작: {}", username); 

        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setWithdrawalReason(reason);
        user.setEmail("deleted_user_" + user.getUsername());
        user.setName("탈퇴회원");

        userRepository.save(user);

        log.info("탈퇴 로직 최종 완료 및 DB 반영: {}", username);

    }

}
