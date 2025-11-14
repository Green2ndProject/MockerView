package com.mockerview.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockerview.dto.UserSearchResponse;
import com.mockerview.entity.User;
import com.mockerview.exception.AlreadyDeletedException;
import com.mockerview.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
    
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void withdraw(String username, String password, String reason) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        if(user.getIsDeleted() == 1){
            throw new AlreadyDeletedException("이미 탈퇴 처리된 계정입니다");
        }

        if(!passwordEncoder.matches(password, user.getPassword())){
            log.warn("탈퇴 로직 실패 - 비밀번호 불일치");
            throw new IllegalArgumentException("비밀번호가 틀렸습니다");
        }

        Long id = user.getId();
        long timestamp = (System.currentTimeMillis() / 1000);
        String anonymizedEmail = String.format("del_%d_%d@mvr.invalid", id, timestamp);
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

    @Transactional(readOnly = true)
    public String findUsername(String name, String email) {
        User user = userRepository.findByNameAndEmail(name, email)
            .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        return user.getUsername();
    }

    @Transactional
    public void resetPassword(String username, String email, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));
        
        if (user.getIsDeleted() == 1) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }
        
        if (!user.getEmail().equals(email)) {
            throw new IllegalArgumentException("이메일이 일치하지 않습니다.");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserSearchResponse> searchUsers(String keyword){

        if(keyword == null || keyword.trim().isEmpty()){
            return Collections.emptyList();
        }

        List<User> users = 
            userRepository.findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword);

        return users.stream().map(UserSearchResponse::from).collect(Collectors.toList());
    }
}
