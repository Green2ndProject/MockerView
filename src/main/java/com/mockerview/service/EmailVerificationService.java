package com.mockerview.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EmailVerificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${admin.email}")
    private String fromEmail;

    private static final String VERIFICATION_PREFIX = "email_verification:";
    private static final String VERIFIED_PREFIX = "email_verified:";
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 10;

    public void sendVerificationCode(String email) {
        String code = generateCode();
        
        redisTemplate.opsForValue().set(
            VERIFICATION_PREFIX + email, 
            code, 
            EXPIRATION_MINUTES, 
            TimeUnit.MINUTES
        );
        
        sendEmail(email, code);
        
        log.info("Verification email sent to: {}", email);
    }

    public boolean verifyCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(VERIFICATION_PREFIX + email);
        
        if (storedCode != null && storedCode.equals(code)) {
            log.info("Email verification successful: {}", email);
            return true;
        }
        
        log.warn("Email verification failed: {}", email);
        return false;
    }
    
    public void markAsVerified(String email) {
        redisTemplate.opsForValue().set(
            VERIFIED_PREFIX + email,
            "true",
            30,
            TimeUnit.MINUTES
        );
        log.info("Email marked as verified: {}", email);
    }
    
    public boolean isVerified(String email) {
        String verified = redisTemplate.opsForValue().get(VERIFIED_PREFIX + email);
        return "true".equals(verified);
    }
    
    public void deleteVerification(String email) {
        redisTemplate.delete(VERIFICATION_PREFIX + email);
        redisTemplate.delete(VERIFIED_PREFIX + email);
        log.info("Verification data deleted for: {}", email);
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("[MockerView] 이메일 인증 코드");
        message.setText(
            "MockerView 회원가입을 환영합니다!\n\n" +
            "아래 인증 코드를 입력하여 이메일 인증을 완료해주세요.\n\n" +
            "인증 코드: " + code + "\n\n" +
            "이 코드는 " + EXPIRATION_MINUTES + "분간 유효합니다.\n\n" +
            "본인이 요청하지 않았다면 이 메일을 무시하세요.\n\n" +
            "감사합니다.\n" +
            "MockerView 팀"
        );
        
        mailSender.send(message);
    }
}