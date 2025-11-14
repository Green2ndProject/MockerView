package com.mockerview.oauth;

import com.mockerview.entity.User;
import com.mockerview.jwt.JWTUtil;
import com.mockerview.repository.UserRepository;
import com.mockerview.service.RefreshTokenService;
import com.mockerview.service.SubscriptionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final SubscriptionService subscriptionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String username = oAuth2User.getUsername();
        
        log.info("OAuth2 로그인 성공: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.isDeleted()) {
            log.warn("탈퇴한 사용자 OAuth2 로그인 시도: {}", username);
            response.sendRedirect("/auth/login?error=deleted");
            return;
        }
        
        boolean needsConsent = user.getAgreePersonalInfo() == null || !user.getAgreePersonalInfo() 
                            || user.getAgreeThirdParty() == null || !user.getAgreeThirdParty();
        
        if (needsConsent) {
            log.info("개인정보 동의 필요: {}", username);
            
            String tempToken = jwtUtil.createJwt(username, user.getRole().toString(), 600000L);
            
            Cookie tempCookie = new Cookie("TempAuthorization", tempToken);
            tempCookie.setMaxAge(600);
            tempCookie.setPath("/");
            tempCookie.setHttpOnly(true);
            
            response.addCookie(tempCookie);
            response.sendRedirect("/auth/oauth-consent");
            return;
        }
        
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
        
        String accessToken = jwtUtil.createJwt(username, user.getRole().toString(), 3600000L);
        String refreshToken = refreshTokenService.createRefreshToken(username);
        
        Cookie accessCookie = new Cookie("Authorization", accessToken);
        accessCookie.setMaxAge(3600);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        
        Cookie refreshCookie = new Cookie("RefreshToken", refreshToken);
        refreshCookie.setMaxAge(604800);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        
        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
        
        log.info("✅ OAuth2 로그인 완료: {}", username);
        response.sendRedirect("/");
    }
}
