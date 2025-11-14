package com.mockerview.controller.api;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.entity.User;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/consent")
@RequiredArgsConstructor
public class ConsentApiController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getConsents(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> consents = new HashMap<>();
        consents.put("agreePersonalInfo", user.getAgreePersonalInfo() != null ? user.getAgreePersonalInfo() : false);
        consents.put("agreeThirdParty", user.getAgreeThirdParty() != null ? user.getAgreeThirdParty() : false);
        consents.put("agreeMarketing", user.getAgreeMarketing() != null ? user.getAgreeMarketing() : false);
        consents.put("agreeMarketingEmail", user.getAgreeMarketingEmail() != null ? user.getAgreeMarketingEmail() : false);
        consents.put("agreeMarketingPush", user.getAgreeMarketingPush() != null ? user.getAgreeMarketingPush() : false);
        consents.put("privacyConsentDate", user.getPrivacyConsentDate());
        
        return ResponseEntity.ok(consents);
    }

    @PutMapping("/marketing")
    public ResponseEntity<?> updateMarketingConsent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Boolean> request) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setAgreeMarketing(request.getOrDefault("agreeMarketing", false));
        user.setAgreeMarketingEmail(request.getOrDefault("agreeMarketingEmail", false));
        user.setAgreeMarketingPush(request.getOrDefault("agreeMarketingPush", false));
        
        userRepository.save(user);
        
        log.info("✅ 마케팅 동의 변경: {} (마케팅={}, 이메일={}, 푸시={})", 
            userDetails.getUsername(),
            request.get("agreeMarketing"),
            request.get("agreeMarketingEmail"),
            request.get("agreeMarketingPush"));
        
        return ResponseEntity.ok()
            .body(Map.of("message", "마케팅 동의가 변경되었습니다."));
    }

    @DeleteMapping("/marketing")
    public ResponseEntity<?> withdrawMarketingConsent(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setAgreeMarketing(false);
        user.setAgreeMarketingEmail(false);
        user.setAgreeMarketingPush(false);
        
        userRepository.save(user);
        
        log.info("✅ 마케팅 동의 철회: {}", userDetails.getUsername());
        
        return ResponseEntity.ok()
            .body(Map.of("message", "마케팅 동의가 철회되었습니다."));
    }
}
