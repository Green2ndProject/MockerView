package com.mockerview.config;

import com.mockerview.entity.User;
import com.mockerview.entity.User.UserRole;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
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
}
