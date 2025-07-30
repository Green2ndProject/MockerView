package com.mydiet.mydiet.config;

import com.mydiet.model.User;
import com.mydiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 테스트 사용자 생성
        if (userRepository.count() == 0) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPassword(hashPassword("password123")); // 간단한 해시 사용
            testUser.setNickname("테스트유저");
            testUser.setEmail("test@example.com");
            testUser.setAge(25);
            testUser.setGender(User.Gender.MALE);
            testUser.setHeight(175);
            testUser.setCurrentWeight(75.0);
            testUser.setWeightGoal(70.0);
            testUser.setEmotionMode(User.EmotionMode.FRIENDLY);
            
            userRepository.save(testUser);
            System.out.println("테스트 사용자가 생성되었습니다.");
        }
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
}