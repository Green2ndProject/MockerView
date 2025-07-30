package com.mydiet.config;

import com.mydiet.model.User;
import com.mydiet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 테스트용 사용자가 없으면 생성
        if (userRepository.count() == 0) {
            User testUser = new User();
            testUser.setNickname("테스트유저");
            testUser.setEmail("test@example.com");
            testUser.setWeightGoal(60.0);
            testUser.setEmotionMode("무자비");
            testUser.setCreatedAt(LocalDateTime.now());
            
            userRepository.save(testUser);
            System.out.println("테스트 사용자 생성 완료: ID = 1");
        }
    }
}
