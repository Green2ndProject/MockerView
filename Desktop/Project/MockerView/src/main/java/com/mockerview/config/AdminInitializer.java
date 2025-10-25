package com.mockerview.config;

import com.mockerview.entity.User;
import com.mockerview.entity.User.UserRole;
import com.mockerview.entity.Subscription;
import com.mockerview.repository.UserRepository;
import com.mockerview.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionRepository subscriptionRepository;
    
    @Value("${admin.username:mockerview}")
    private String adminUsername;
    
    @Value("${admin.password:mockerview}")
    private String adminPassword;
    
    @Value("${admin.name:MockerView}")
    private String adminName;
    
    @Value("${admin.email:admin@mockerview.com}")
    private String adminEmail;
    
    @Bean
    public CommandLineRunner initAdmin() {
        return args -> {
            try {
                if (userRepository.findByUsername(adminUsername).isEmpty()) {
                    User admin = User.builder()
                            .username(adminUsername)
                            .password(passwordEncoder.encode(adminPassword))
                            .name(adminName)
                            .email(adminEmail)
                            .role(UserRole.ADMIN)
                            .build();
                    
                    userRepository.save(admin);
                    log.info("✅ Admin account created: {}", adminUsername);
                } else {
                    log.info("ℹ️ Admin account already exists: {}", adminUsername);
                }
            } catch (Exception e) {
                log.error("❌ Failed to create admin account", e);
            }
        };
    }
    
    @Bean
    public CommandLineRunner ensureAllUsersHaveSubscription() {
        return args -> {
            try {
                List<User> allUsers = userRepository.findAll();
                int createdCount = 0;
                
                for (User user : allUsers) {
                    Optional<Subscription> existingSub = subscriptionRepository
                        .findByUserAndStatus(user, Subscription.SubscriptionStatus.ACTIVE);
                    
                    if (existingSub.isEmpty()) {
                        Subscription freeSub = Subscription.builder()
                            .user(user)
                            .planType(Subscription.PlanType.FREE)
                            .status(Subscription.SubscriptionStatus.ACTIVE)
                            .sessionLimit(5)
                            .usedSessions(0)
                            .autoRenew(true)
                            .startDate(LocalDateTime.now())
                            .endDate(LocalDateTime.now().plusMonths(1))
                            .nextBillingDate(LocalDateTime.now().plusMonths(1))
                            .build();
                        
                        subscriptionRepository.save(freeSub);
                        createdCount++;
                        log.info("✅ FREE 구독 생성: {}", user.getUsername());
                    }
                }
                
                if (createdCount > 0) {
                    log.info("📊 총 {}명에게 FREE 구독 생성 완료", createdCount);
                } else {
                    log.info("ℹ️ 모든 사용자가 구독을 보유하고 있습니다");
                }
            } catch (Exception e) {
                log.error("❌ 구독 초기화 실패", e);
            }
        };
    }
}