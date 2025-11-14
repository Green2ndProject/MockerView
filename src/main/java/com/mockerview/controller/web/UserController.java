package com.mockerview.controller.web;

import com.mockerview.annotation.RateLimit;
import com.mockerview.dto.FindUsernameRequest;
import com.mockerview.dto.RegisterDTO;
import com.mockerview.dto.ResetPasswordRequest;
import com.mockerview.entity.User;
import com.mockerview.jwt.JWTUtil;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.EmailVerificationService;
import com.mockerview.service.RecaptchaService;
import com.mockerview.service.UserService;
import com.mockerview.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Autowired
    private RecaptchaService recaptchaService;
    
    @Autowired
    private JWTUtil jwtUtil;

    @GetMapping("/login")
    public String loginForm() {
        log.info("로그인폼 controller 진입 성공!");
        return "user/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "user/register";
    }

    @GetMapping("/check-username")
    @ResponseBody
    @RateLimit(limit = 20, duration = 60, message = "아이디 중복 확인 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("available", false, "message", "아이디를 입력해주세요."));
        }
        
        if (username.length() < 4 || username.length() > 20) {
            return ResponseEntity.badRequest()
                .body(Map.of("available", false, "message", "아이디는 4-20자여야 합니다."));
        }
        
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            return ResponseEntity.badRequest()
                .body(Map.of("available", false, "message", "영문과 숫자만 사용 가능합니다."));
        }
        
        boolean available = !userRepository.existsActiveByUsername(username);
        
        return ResponseEntity.ok()
            .body(Map.of("available", available));
    }

    @PostMapping("/send-verification")
    @ResponseBody
    @RateLimit(limit = 3, duration = 600, message = "이메일 인증 요청이 너무 많습니다. 10분 후 다시 시도해주세요.")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "이메일을 입력해주세요."));
        }
        
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "올바른 이메일 형식이 아닙니다."));
        }
        
        if (userRepository.existsActiveByEmail(email)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "이미 사용 중인 이메일입니다."));
        }
        
        try {
            emailVerificationService.sendVerificationCode(email);
            return ResponseEntity.ok()
                .body(Map.of("message", "인증 코드가 발송되었습니다."));
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "이메일 전송에 실패했습니다."));
        }
    }

    @PostMapping("/verify-email")
    @ResponseBody
    @RateLimit(limit = 10, duration = 300, message = "인증 시도가 너무 많습니다. 5분 후 다시 시도해주세요.")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        if (email == null || code == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("valid", false, "message", "이메일과 코드를 입력해주세요."));
        }
        
        boolean valid = emailVerificationService.verifyCode(email, code);
        
        if (valid) {
            emailVerificationService.markAsVerified(email);
            return ResponseEntity.ok()
                .body(Map.of("valid", true, "message", "이메일 인증이 완료되었습니다."));
        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("valid", false, "message", "인증 코드가 올바르지 않거나 만료되었습니다."));
        }
    }

    @PostMapping("/register")
    @ResponseBody
    @RateLimit(limit = 5, duration = 3600, message = "회원가입 시도가 너무 많습니다. 1시간 후 다시 시도해주세요.")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO) {
        
        log.info("회원가입 요청: username={}, email={}, role={}", 
            registerDTO.getUsername(), 
            registerDTO.getEmail(), 
            registerDTO.getRole());
        
        String recaptchaToken = registerDTO.getRecaptchaToken();
        if (!recaptchaService.verify(recaptchaToken)) {
            log.warn("reCAPTCHA 검증 실패: email={}", registerDTO.getEmail());
            return ResponseEntity.badRequest()
                .body(Map.of("message", "봇 검증에 실패했습니다. 다시 시도해주세요."));
        }
        
        String username = registerDTO.getUsername();
        String password = registerDTO.getPassword();
        String name = registerDTO.getName();
        String email = registerDTO.getEmail();
        String role = registerDTO.getRole();

        if (username == null || password == null || name == null || email == null || role == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "모든 필드를 입력해주세요."));
        }
        
        if (!emailVerificationService.isVerified(email)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "이메일 인증을 완료해주세요."));
        }
        
        if (registerDTO.getAgreePersonalInfo() == null || !registerDTO.getAgreePersonalInfo()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "개인정보 수집 및 이용에 동의해주세요."));
        }
        
        if (registerDTO.getAgreeThirdParty() == null || !registerDTO.getAgreeThirdParty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "개인정보 제3자 제공에 동의해주세요."));
        }

        Optional<User> deletedUserByUsername = userRepository.findByUsername(username)
            .filter(User::isDeleted);
        
        Optional<User> deletedUserByEmail = userRepository.findByEmail(email)
            .filter(User::isDeleted);
        
        if (deletedUserByUsername.isPresent() || deletedUserByEmail.isPresent()) {
            User deletedUser = deletedUserByUsername.orElse(deletedUserByEmail.get());
            
            log.info("🔄 탈퇴한 회원 재가입 처리 - email: {}, username: {}, userId: {}", 
                email, username, deletedUser.getId());
            
            deletedUser.setDeletedAt(null);
            deletedUser.setIsDeleted(0);
            deletedUser.setEmail(email);
            deletedUser.setUsername(username);
            deletedUser.setName(name);
            deletedUser.setPassword(passwordEncoder.encode(password));
            deletedUser.setRole(User.UserRole.valueOf(role));
            deletedUser.setWithdrawalReason(null);
            deletedUser.setAgreePersonalInfo(registerDTO.getAgreePersonalInfo() != null ? registerDTO.getAgreePersonalInfo() : false);
            deletedUser.setAgreeThirdParty(registerDTO.getAgreeThirdParty() != null ? registerDTO.getAgreeThirdParty() : false);
            deletedUser.setAgreeMarketing(registerDTO.getAgreeMarketing() != null ? registerDTO.getAgreeMarketing() : false);
            deletedUser.setAgreeMarketingEmail(registerDTO.getAgreeMarketingEmail() != null ? registerDTO.getAgreeMarketingEmail() : false);
            deletedUser.setAgreeMarketingPush(registerDTO.getAgreeMarketingPush() != null ? registerDTO.getAgreeMarketingPush() : false);
            deletedUser.setPrivacyConsentDate(LocalDateTime.now());
            
            User reactivatedUser = userRepository.save(deletedUser);
            log.info("✅ 회원 재활성화 완료 - userId: {}", reactivatedUser.getId());
            
            subscriptionService.createFreeSubscription(reactivatedUser);
            log.info("✅ FREE 구독 재생성 완료 - userId: {}", reactivatedUser.getId());
            
            emailVerificationService.deleteVerification(email);
            
            return ResponseEntity.ok()
                .body(Map.of("message", "회원가입이 완료되었습니다."));
        }

        if (userRepository.existsActiveByUsername(username)) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }

        if (userRepository.existsActiveByEmail(email)) {
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
                            .agreePersonalInfo(registerDTO.getAgreePersonalInfo() != null ? registerDTO.getAgreePersonalInfo() : false)
                            .agreeThirdParty(registerDTO.getAgreeThirdParty() != null ? registerDTO.getAgreeThirdParty() : false)
                            .agreeMarketing(registerDTO.getAgreeMarketing() != null ? registerDTO.getAgreeMarketing() : false)
                            .agreeMarketingEmail(registerDTO.getAgreeMarketingEmail() != null ? registerDTO.getAgreeMarketingEmail() : false)
                            .agreeMarketingPush(registerDTO.getAgreeMarketingPush() != null ? registerDTO.getAgreeMarketingPush() : false)
                            .privacyConsentDate(LocalDateTime.now())
                            .build();
            
            User savedUser = userRepository.save(user);
            log.info("회원가입 성공: {} (동의: 개인정보={}, 제3자={}, 마케팅={})", 
                username, 
                registerDTO.getAgreePersonalInfo(), 
                registerDTO.getAgreeThirdParty(), 
                registerDTO.getAgreeMarketing());
            
            subscriptionService.createFreeSubscription(savedUser);
            log.info("FREE 구독 생성 완료: userId={}", savedUser.getId());
            
            emailVerificationService.deleteVerification(email);
            
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

    @GetMapping("/oauth-consent")
    public String oauthConsentPage() {
        return "user/oauth-consent";
    }

    @PostMapping("/oauth-consent")
    @ResponseBody
    public ResponseEntity<?> processOAuthConsent(@RequestBody Map<String, Boolean> consents,
                                                  @CookieValue(value = "TempAuthorization", required = false) String tempToken) {
        if (tempToken == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "인증 정보가 없습니다."));
        }
        
        try {
            String username = jwtUtil.getUsername(tempToken);
            
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!consents.getOrDefault("agreePersonalInfo", false) || 
                !consents.getOrDefault("agreeThirdParty", false)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "필수 동의 항목에 동의해주세요."));
            }
            
            user.setAgreePersonalInfo(consents.getOrDefault("agreePersonalInfo", false));
            user.setAgreeThirdParty(consents.getOrDefault("agreeThirdParty", false));
            user.setAgreeMarketing(consents.getOrDefault("agreeMarketing", false));
            user.setAgreeMarketingEmail(consents.getOrDefault("agreeMarketingEmail", false));
            user.setAgreeMarketingPush(consents.getOrDefault("agreeMarketingPush", false));
            user.setPrivacyConsentDate(LocalDateTime.now());
            user.setLastLoginDate(LocalDateTime.now());
            
            userRepository.save(user);
            
            log.info("✅ OAuth2 동의 완료: {} (개인정보={}, 제3자={}, 마케팅={})", 
                username, 
                consents.get("agreePersonalInfo"), 
                consents.get("agreeThirdParty"), 
                consents.get("agreeMarketing"));
            
            return ResponseEntity.ok()
                .body(Map.of("message", "동의 처리가 완료되었습니다."));
        } catch (Exception e) {
            log.error("OAuth2 동의 처리 실패", e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "동의 처리 중 오류가 발생했습니다."));
        }
    }
}
