package com.mockerview.service;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockerview.entity.User;
import com.mockerview.exception.AlreadyDeletedException;
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

        User user                 = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        // 멱등성 검사
        if(user.getIsDeleted() == 1){
            throw new AlreadyDeletedException("이미 탈퇴 처리된 계정입니다"); // 409 처리
        }

        if(!passwordEncoder.matches(password, user.getPassword())){
            log.warn("탈퇴 로직 실패 - 비밀번호 불일치");
            throw new IllegalArgumentException("비밀번호가 틀렸습니다");
        }

        // 익명화 포맷 처리
        Long id                   = user.getId();
        long timestamp            = (System.currentTimeMillis() / 1000) ;
        String anonymizedEmail    = String.format("del_%d_%d@mvr.invalid", id, timestamp);
        String anonymizedUsername = String.format("del_user_%d_%d", id, timestamp);
        
        log.info("탈퇴 로직 - Service 진입 성공."); 

        log.info("탈퇴 로직 성공 - Soft Delete 처리 시작"); 
 
        user.setDeletedAt(LocalDateTime.now());
        user.setPassword("invalid_deleted_hash_" + id);
        user.setWithdrawalReason(reason);
        user.setEmail(anonymizedEmail);
        user.setUsername(anonymizedUsername);
        user.setName("탈퇴회원");
        user.setIsDeleted(1);

        userRepository.save(user);

        log.info("탈퇴 로직 최종 완료 및 DB 반영");

    }

}
